package com.fabriciot.vo.audit;

import lombok.Data;

import java.util.Map;

@Data
public class AuditLogVO {

    private String logId;
    private String operationTime;
    private String operator;
    private String operationType;
    private String deviceId;
    private String onChain;
    private String txHash;
    private String remark;
    private Map<String, Object> detail;
}

