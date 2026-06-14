package com.exam.system.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String module;
    private String action;
    private String method;
    private String path;
    private String ip;
    private String detail;
    private Boolean success;
    private LocalDateTime createTime;
}
