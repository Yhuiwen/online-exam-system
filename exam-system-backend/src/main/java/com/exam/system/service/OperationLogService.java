package com.exam.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.entity.OperationLog;

public interface OperationLogService {
    void record(OperationLog log);

    Page<OperationLog> page(long pageNum, long pageSize, String keyword, String module);
}
