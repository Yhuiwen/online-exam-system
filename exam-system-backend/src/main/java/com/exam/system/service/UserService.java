package com.exam.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.dto.ResetPasswordRequest;
import com.exam.system.dto.UserCreateRequest;
import com.exam.system.dto.UserUpdateRequest;
import com.exam.system.vo.UserVO;

public interface UserService {
    Page<UserVO> page(long pageNum, long pageSize, String keyword, String role, Integer status);

    UserVO create(UserCreateRequest request);

    UserVO update(Long id, UserUpdateRequest request);

    void resetPassword(Long id, ResetPasswordRequest request);

    void updateStatus(Long id, Integer status);
}
