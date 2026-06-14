package com.exam.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.system.entity.OperationLog;
import com.exam.system.mapper.OperationLogMapper;
import com.exam.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {
    private final OperationLogMapper mapper;

    @Override
    public void record(OperationLog log) {
        mapper.insert(log);
    }

    @Override
    public Page<OperationLog> page(long pageNum, long pageSize, String keyword, String module) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime);
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getRealName, keyword)
                    .or().like(OperationLog::getAction, keyword)
                    .or().like(OperationLog::getDetail, keyword));
        }
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
