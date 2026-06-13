package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String realName;
    private String role;
    private String email;
    private String phone;
    private Integer status;
}
