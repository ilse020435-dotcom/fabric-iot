package com.fabriciot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_device_lifecycle_event")
public class IotDeviceLifecycleEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;

    @TableField("operation_type")
    private String operationType;

    @TableField("before_status")
    private String beforeStatus;

    @TableField("after_status")
    private String afterStatus;

    @TableField("summary_hash")
    private String summaryHash;

    private String operator;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("tx_hash")
    private String txHash;

    @TableLogic
    private Integer deleted;
}

