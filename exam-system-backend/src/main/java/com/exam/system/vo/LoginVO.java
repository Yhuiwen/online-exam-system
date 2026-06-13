package com.exam.system.vo;
import com.exam.system.entity.SysUser;
public record LoginVO(String token, SysUser user) {}
