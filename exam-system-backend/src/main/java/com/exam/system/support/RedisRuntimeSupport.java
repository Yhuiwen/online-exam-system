package com.exam.system.support;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisRuntimeSupport implements RuntimeSupport {
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
              return 0
            end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryRateLimit(String scope, String key, int maxAttempts, int windowSeconds) {
        String redisKey = "rate:" + scope + ":" + key;
        Long allowed = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(redisKey),
                String.valueOf(windowSeconds),
                String.valueOf(maxAttempts));
        return allowed != null && allowed == 1L;
    }

    @Override
    public boolean markIfAbsent(String key, Duration ttl) {
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(created);
    }

    @Override
    public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
        String lockKey = "lock:" + key;
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalStateException("资源繁忙，请稍后重试");
        }
        try {
            return action.get();
        } finally {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), token);
        }
    }

    @Override
    public String getCache(String key) {
        return redisTemplate.opsForValue().get("cache:" + key);
    }

    @Override
    public void putCache(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set("cache:" + key, value, ttl);
    }

    @Override
    public void evictCache(String key) {
        redisTemplate.delete("cache:" + key);
    }
}
