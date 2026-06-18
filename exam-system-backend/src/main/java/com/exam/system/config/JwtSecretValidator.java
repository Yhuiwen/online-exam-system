package com.exam.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class JwtSecretValidator {
    static final String DEFAULT_SECRET = "exam-system-course-project-jwt-secret-key-2026-change-me";
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String secret;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("生产环境必须配置 JWT_SECRET 环境变量");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("生产环境 JWT_SECRET 长度不能少于 " + MIN_SECRET_LENGTH + " 个字符");
        }
        if (DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("生产环境不能使用默认 JWT_SECRET，请设置强随机密钥");
        }
    }
}
