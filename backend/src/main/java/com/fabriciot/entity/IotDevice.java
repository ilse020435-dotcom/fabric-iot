package com.fabriciot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("iot_device")
public class IotDevice {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;

    @TableField("device_name")
    private String deviceName;

    private String did;

    @TableField("device_type")
    private String deviceType;

    private String vendor;

    @TableField("lifecycle_status")
    private String lifecycleStatus;

    private String description;

    @TableLogic
    private Integer deleted;

    @TableField("register_time")
    private LocalDateTime registerTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

