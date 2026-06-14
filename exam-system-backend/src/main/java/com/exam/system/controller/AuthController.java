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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OperationLogService operationLogService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (count > 0) throw new BusinessException("用户名已存在");
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
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != 1) throw new BusinessException(403, "账号已被禁用");
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        recordLogin(user, true, "登录成功");
        return Result.success(new LoginVO(token, user));
    }

    private void recordLogin(SysUser user, boolean success, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(user == null ? null : user.getId());
        log.setUsername(user == null ? null : user.getUsername());
        log.setRealName(user == null ? null : user.getRealName());
        log.setModule("认证");
        log.setAction("登录");
        log.setMethod("POST");
        log.setPath("/api/auth/login");
        log.setDetail(detail);
        log.setSuccess(success);
        log.setCreateTime(LocalDateTime.now());
        operationLogService.record(log);
    }

    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.success(SecurityUtils.current().getUser());
    }
}
