package com.fabriciot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_device_status_snapshot")
public class IotDeviceStatusSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;

    @TableField("monitor_status")
    private String monitorStatus;

    @TableField("signal_strength")
    private String signalStrength;

    private String temperature;

    @TableField("summary_hash")
    private String summaryHash;

    @TableField("block_height")
    private Long blockHeight;

    @TableField("tx_hash")
    private String txHash;

    @TableLogic
    private Integer deleted;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

