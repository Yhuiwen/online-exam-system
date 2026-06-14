package com.exam.system.config;

import com.exam.system.dto.LoginRequest;
import com.exam.system.entity.OperationLog;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Pointcut("within(com.exam.system.controller..*) && !within(com.exam.system.controller.OperationLogController)")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut() && (@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        if (pathContainsLogin(request.getRequestURI())) {
            return joinPoint.proceed();
        }
        OperationLog log = buildBaseLog(joinPoint, request);
        try {
            Object result = joinPoint.proceed();
            log.setSuccess(true);
            operationLogService.record(log);
            return result;
        } catch (Throwable ex) {
            log.setSuccess(false);
            log.setDetail(truncate(log.getDetail() + " | 异常: " + ex.getMessage()));
            operationLogService.record(log);
            throw ex;
        }
    }

    private OperationLog buildBaseLog(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OperationLog log = new OperationLog();
        try {
            var user = SecurityUtils.current().getUser();
            log.setUserId(user.getId());
            log.setUsername(user.getUsername());
            log.setRealName(user.getRealName());
        } catch (Exception ignored) {
            // Public endpoints such as login may not have an authenticated user yet.
        }
        log.setModule(resolveModule(request.getRequestURI()));
        log.setAction(resolveAction(method, joinPoint, request.getRequestURI()));
        log.setMethod(request.getMethod());
        log.setPath(request.getRequestURI());
        log.setIp(resolveIp(request));
        log.setDetail(truncate(buildDetail(request.getRequestURI(), joinPoint.getArgs())));
        if (pathContainsLogin(request.getRequestURI()) && joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof LoginRequest loginRequest) {
            log.setUsername(loginRequest.username());
            log.setAction("登录");
        }
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    private String resolveModule(String path) {
        if (path.startsWith("/api/auth")) return "认证";
        if (path.startsWith("/api/users")) return "用户管理";
        if (path.startsWith("/api/courses")) return "课程管理";
        if (path.startsWith("/api/questions")) return "题库管理";
        if (path.startsWith("/api/exams")) return "考试管理";
        if (path.startsWith("/api/student-exams")) return "学生考试";
        if (path.startsWith("/api/review")) return "主观题批改";
        if (path.startsWith("/api/exam-violation")) return "考试监控";
        if (path.startsWith("/api/statistics")) return "统计分析";
        if (path.startsWith("/api/civil-service")) return "公考刷题";
        return "系统";
    }

    private String resolveAction(Method method, ProceedingJoinPoint joinPoint, String path) {
        if (pathContainsLogin(path)) return "登录";
        if (method.isAnnotationPresent(PostMapping.class)) return "新增/提交";
        if (method.isAnnotationPresent(PutMapping.class)) return "修改";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "删除";
        return joinPoint.getSignature().getName();
    }

    private boolean pathContainsLogin(String path) {
        return path != null && path.contains("/auth/login");
    }

    private String buildDetail(String path, Object[] args) {
        if (path.contains("/auth/login")) {
            return "用户登录";
        }
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(args.length == 1 ? args[0] : args);
            return json.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
        } catch (Exception ex) {
            return "";
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
