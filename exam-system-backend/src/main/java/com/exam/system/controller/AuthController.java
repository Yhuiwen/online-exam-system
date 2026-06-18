package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.common.Result;
import com.exam.system.dto.LoginRequest;
import com.exam.system.dto.RegisterRequest;
import com.exam.system.entity.OperationLog;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.SysUserMapper;
import com.exam.system.security.JwtUtil;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.OperationLogService;
import com.exam.system.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "TEACHER", "STUDENT");

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OperationLogService operationLogService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole("STUDENT");
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        String username = request.username().trim();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null || !StringUtils.hasText(user.getPassword()) ||
                !passwordMatches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }
        if (user.getStatus() == null) {
            throw new BusinessException(403, "账号状态异常，请联系管理员");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (user.getId() == null || !StringUtils.hasText(user.getUsername()) ||
                !StringUtils.hasText(user.getRole()) || !VALID_ROLES.contains(user.getRole())) {
            throw new BusinessException(403, "账号信息异常，请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        recordLogin(user, true, "登录成功");
        return Result.success(new LoginVO(token, user));
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (RuntimeException e) {
            log.warn("Password verification failed because stored credential is invalid", e);
            return false;
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(401, "用户名或密码错误");
    }

    private void recordLogin(SysUser user, boolean success, String detail) {
        try {
            OperationLog logEntry = new OperationLog();
            logEntry.setUserId(user == null ? null : user.getId());
            logEntry.setUsername(user == null ? null : user.getUsername());
            logEntry.setRealName(user == null ? null : user.getRealName());
            logEntry.setModule("认证");
            logEntry.setAction("登录");
            logEntry.setMethod("POST");
            logEntry.setPath("/api/auth/login");
            logEntry.setDetail(detail);
            logEntry.setSuccess(success);
            logEntry.setCreateTime(LocalDateTime.now());
            operationLogService.record(logEntry);
        } catch (Exception e) {
            log.warn("Failed to record login operation log for userId={}", user == null ? null : user.getId(), e);
        }
    }

    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.success(SecurityUtils.current().getUser());
    }
}
