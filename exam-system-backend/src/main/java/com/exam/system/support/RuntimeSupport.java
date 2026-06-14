package com.exam.system.support;

import java.time.Duration;
import java.util.function.Supplier;

public interface RuntimeSupport {
    boolean tryRateLimit(String scope, String key, int maxAttempts, int windowSeconds);

    boolean markIfAbsent(String key, Duration ttl);

    <T> T withLock(String key, Duration ttl, Supplier<T> action);

    String getCache(String key);

    void putCache(String key, String value, Duration ttl);

    void evictCache(String key);
}
