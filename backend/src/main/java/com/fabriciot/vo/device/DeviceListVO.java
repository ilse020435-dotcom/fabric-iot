package com.fabriciot.vo.device;

import lombok.Data;

@Data
public class DeviceListVO {

    private String deviceId;
    private String deviceName;
    private String did;
    private String deviceType;
    private String vendor;
    private String lifecycleStatus;
    private String registerTime;
    private String description;
}

