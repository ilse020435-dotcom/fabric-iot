package com.fabriciot.vo.monitor;

import lombok.Data;

@Data
public class MonitorRecordVO {

    private String deviceId;
    private String deviceName;
    private String monitorStatus;
    private String updatedAt;
    private String summaryHash;
    private Long blockHeight;
    private String txHash;
    private String signalStrength;
    private String temperature;
}

