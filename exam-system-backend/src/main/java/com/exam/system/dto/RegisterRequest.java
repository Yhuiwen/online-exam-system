package com.exam.system.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record RegisterRequest(@NotBlank(message = "用户名不能为空") String username,
                              @NotBlank(message = "密码不能为空") String password,
                              @NotBlank(message = "姓名不能为空") String realName,
                              @Email(message = "邮箱格式错误") String email,
                              String phone) {}
