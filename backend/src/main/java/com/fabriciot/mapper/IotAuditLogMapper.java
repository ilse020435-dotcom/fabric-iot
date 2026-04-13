package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fabriciot.entity.IotAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IotAuditLogMapper extends BaseMapper<IotAuditLog> {

    @Results(id = "auditLogResultMap", value = {
            @Result(column = "detail_json", property = "detailJson", typeHandler = JacksonTypeHandler.class)
    })
    @Select("""
            SELECT *
            FROM iot_audit_log
            WHERE log_id = #{logId}
              AND deleted = 0
            LIMIT 1
            """)
    IotAuditLog selectByLogId(@Param("logId") String logId);

    @ResultMap("auditLogResultMap")
    @Select("""
            SELECT *
            FROM iot_audit_log
            WHERE tx_hash = #{txHash}
              AND deleted = 0
            ORDER BY operation_time DESC
            LIMIT 1
            """)
    IotAuditLog selectByTxHash(@Param("txHash") String txHash);
}

