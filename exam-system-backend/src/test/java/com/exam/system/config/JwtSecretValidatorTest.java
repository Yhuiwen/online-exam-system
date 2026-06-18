package com.exam.system.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretValidatorTest {

    @Test
    void rejectsBlankSecret() {
        JwtSecretValidator validator = new JwtSecretValidator();
        ReflectionTestUtils.setField(validator, "secret", "   ");
        assertThrows(IllegalStateException.class, validator::validateSecret);
    }

    @Test
    void rejectsDefaultSecret() {
        JwtSecretValidator validator = new JwtSecretValidator();
        ReflectionTestUtils.setField(validator, "secret", JwtSecretValidator.DEFAULT_SECRET);
        assertThrows(IllegalStateException.class, validator::validateSecret);
    }

    @Test
    void rejectsShortSecret() {
        JwtSecretValidator validator = new JwtSecretValidator();
        ReflectionTestUtils.setField(validator, "secret", "too-short-secret-value");
        assertThrows(IllegalStateException.class, validator::validateSecret);
    }

    @Test
    void acceptsStrongCustomSecret() {
        JwtSecretValidator validator = new JwtSecretValidator();
        ReflectionTestUtils.setField(validator, "secret",
                "prod-only-jwt-secret-with-enough-random-length-2026");
        assertDoesNotThrow(validator::validateSecret);
    }
}
