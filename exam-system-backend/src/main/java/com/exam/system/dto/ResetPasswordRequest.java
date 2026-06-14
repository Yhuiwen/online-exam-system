package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "新密码长度至少为 6 位")
        String newPassword) {
}
