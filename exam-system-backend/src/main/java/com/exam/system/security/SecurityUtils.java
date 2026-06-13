package com.exam.system.security;

import com.exam.system.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static LoginUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return loginUser;
    }

    public static Long userId() {
        return current().getUser().getId();
    }
}
