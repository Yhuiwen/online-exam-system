package com.exam.system.exception;

import com.exam.system.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionUsesMatchingHttpStatus() {
        ResponseEntity<Result<Void>> response = handler.handleBusiness(new BusinessException(403, "无权访问"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getCode());
        assertEquals("无权访问", response.getBody().getMessage());
    }

    @Test
    void unknownExceptionReturnsGenericMessage() {
        ResponseEntity<Result<Void>> response = handler.handleOther(new RuntimeException("database password leaked"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("服务器内部错误，请联系管理员", response.getBody().getMessage());
    }
}
