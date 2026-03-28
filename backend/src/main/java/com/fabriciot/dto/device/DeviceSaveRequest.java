package com.fabriciot.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceSaveRequest {

    private String deviceId;

    private String did;

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    @NotBlank(message = "厂商不能为空")
    private String vendor;

    private String description;
}

