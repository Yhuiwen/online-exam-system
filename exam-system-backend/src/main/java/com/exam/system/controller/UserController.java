package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.common.Result;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final SysUserMapper mapper;

    @GetMapping
    public Result<Page<SysUser>> list(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword))
                .orderByDesc(SysUser::getCreateTime);
        return Result.success(mapper.selectPage(Page.of(page, size), query));
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = mapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setStatus(status);
        mapper.updateById(user);
        return Result.success();
    }
}
