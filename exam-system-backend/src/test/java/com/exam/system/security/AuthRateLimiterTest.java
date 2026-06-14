package com.exam.system.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthRateLimiterTest {
    @Test
    void blocksAfterMaxAttemptsWithinWindow() {
        AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofSeconds(60));

        assertTrue(limiter.tryAcquire("127.0.0.1"));
        assertTrue(limiter.tryAcquire("127.0.0.1"));
        assertTrue(limiter.tryAcquire("127.0.0.1"));
        assertFalse(limiter.tryAcquire("127.0.0.1"));
        assertTrue(limiter.tryAcquire("127.0.0.2"));
    }
}
