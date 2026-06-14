package com.exam.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名不能超过 50 个字符")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码长度至少为 6 位")
        String password,
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过 50 个字符")
        String realName,
        @NotBlank(message = "角色不能为空")
        String role,
        @Email(message = "邮箱格式错误")
        @Size(max = 100, message = "邮箱不能超过 100 个字符")
        String email,
        @Size(max = 20, message = "手机号不能超过 20 个字符")
        String phone) {
}
