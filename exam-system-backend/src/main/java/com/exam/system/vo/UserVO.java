package com.exam.system.vo;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String realName,
        String role,
        String email,
        String phone,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
