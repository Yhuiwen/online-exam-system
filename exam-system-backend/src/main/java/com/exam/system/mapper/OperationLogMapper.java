package com.exam.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
