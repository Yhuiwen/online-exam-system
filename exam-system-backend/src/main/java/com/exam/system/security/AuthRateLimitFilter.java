package com.exam.system.security;

import com.exam.system.common.Result;
import com.exam.system.support.RuntimeSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final RuntimeSupport runtimeSupport;

    @Value("${auth.rate-limit.max-attempts:10}")
    private int maxAttempts;

    @Value("${auth.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!"/api/auth/login".equals(path) && !"/api/auth/register".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!runtimeSupport.tryRateLimit("auth", clientKey(request), maxAttempts, windowSeconds)) {
            writeError(response, 429, "请求过于频繁，请稍后再试");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), Result.error(code, message));
    }
}
