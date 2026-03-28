package com.fabriciot.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceStatusReportRequest {

    @NotBlank(message = "monitorStatus 不能为空")
    private String monitorStatus;

    private String signalStrength;

    private String temperature;
}
