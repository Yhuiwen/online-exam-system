package com.exam.system.support;

import com.exam.system.security.AuthRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRuntimeSupport implements RuntimeSupport {
    private final Map<String, AuthRateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final Map<String, Long> dedupMarks = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryRateLimit(String scope, String key, int maxAttempts, int windowSeconds) {
        String limiterKey = scope + ":" + key;
        AuthRateLimiter limiter = rateLimiters.computeIfAbsent(limiterKey,
                ignored -> new AuthRateLimiter(maxAttempts, Duration.ofSeconds(windowSeconds)));
        return limiter.tryAcquire("default");
    }

    @Override
    public boolean markIfAbsent(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        Long expiresAt = dedupMarks.get(key);
        if (expiresAt != null && expiresAt > now) {
            return false;
        }
        dedupMarks.put(key, now + ttl.toMillis());
        return true;
    }

    @Override
    public <T> T withLock(String key, Duration ttl, Supplier<T> action) {
        Object lock = locks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }

    @Override
    public String getCache(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
            cache.remove(key);
            return null;
        }
        return entry.value();
    }

    @Override
    public void putCache(String key, String value, Duration ttl) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public void evictCache(String key) {
        cache.remove(key);
    }

    private record CacheEntry(String value, long expiresAt) {
    }
}
