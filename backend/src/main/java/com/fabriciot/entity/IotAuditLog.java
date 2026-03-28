package com.fabriciot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "iot_audit_log", autoResultMap = true)
public class IotAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("log_id")
    private String logId;

    @TableField("operation_time")
    private LocalDateTime operationTime;

    private String operator;

    @TableField("operation_type")
    private String operationType;

    @TableField("device_id")
    private String deviceId;

    @TableField("on_chain")
    private Integer onChain;

    @TableField("tx_hash")
    private String txHash;

    private String remark;

    @TableField(value = "detail_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> detailJson;

    @TableLogic
    private Integer deleted;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

