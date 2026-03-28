package com.fabriciot.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceStatusUpdateRequest {

    @NotBlank(message = "lifecycleStatus 不能为空")
    private String lifecycleStatus;
}
