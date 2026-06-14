package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.dto.ResetPasswordRequest;
import com.exam.system.dto.UserCreateRequest;
import com.exam.system.dto.UserUpdateRequest;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.SysUserMapper;
import com.exam.system.security.SecurityUtils;
import com.exam.system.service.UserService;
import com.exam.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Set<String> ROLES = Set.of("ADMIN", "TEACHER", "STUDENT");

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> page(long pageNum, long pageSize, String keyword, String role, Integer status) {
        validateOptionalRole(role);
        validateOptionalStatus(status);
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .and(hasText(keyword), wrapper -> wrapper.like(SysUser::getUsername, keyword.trim())
                        .or().like(SysUser::getRealName, keyword.trim()))
                .eq(hasText(role), SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> users = userMapper.selectPage(Page.of(pageNum, pageSize), query);
        Page<UserVO> result = Page.of(users.getCurrent(), users.getSize(), users.getTotal());
        result.setRecords(users.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    @Transactional
    public UserVO create(UserCreateRequest request) {
        String username = request.username().trim();
        validateRole(request.role());
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName().trim());
        user.setRole(request.role());
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        user.setStatus(1);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        validateRole(request.role());
        validateStatus(request.status());
        ensureNotDisablingSelf(id, request.status());
        user.setRealName(request.realName().trim());
        user.setRole(request.role());
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        user.setStatus(request.status());
        userMapper.updateById(user);
        return toVO(userMapper.selectById(id));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        ensureNotDisablingSelf(id, status);
        SysUser user = requireUser(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    private void ensureNotDisablingSelf(Long id, Integer status) {
        if (status == 0 && SecurityUtils.userId().equals(id)) {
            throw new BusinessException("不能禁用当前登录管理员");
        }
    }

    private void validateRole(String role) {
        if (!ROLES.contains(role)) throw new BusinessException("用户角色不合法");
    }

    private void validateOptionalRole(String role) {
        if (hasText(role)) validateRole(role);
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("用户状态只能是 0 或 1");
        }
    }

    private void validateOptionalStatus(Integer status) {
        if (status != null) validateStatus(status);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private UserVO toVO(SysUser user) {
        return new UserVO(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole(),
                user.getEmail(), user.getPhone(), user.getStatus(),
                user.getCreateTime(), user.getUpdateTime()
        );
    }
}
