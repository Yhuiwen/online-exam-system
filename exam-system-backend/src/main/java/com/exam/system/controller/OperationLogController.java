package com.exam.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.common.Result;
import com.exam.system.entity.OperationLog;
import com.exam.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<Page<OperationLog>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module) {
        return Result.success(operationLogService.page(pageNum, pageSize, keyword, module));
    }
}
