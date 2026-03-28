package com.fabriciot.vo.device;

import lombok.Data;

@Data
public class LifecycleEventVO {

    private String operationType;
    private String beforeStatus;
    private String afterStatus;
    private String summaryHash;
    private String operator;
    private String occurredAt;
    private String txHash;
}

