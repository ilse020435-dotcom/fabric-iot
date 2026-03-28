package com.fabriciot.service;

import com.fabriciot.entity.IotAuditLog;

import java.time.LocalDateTime;
import java.util.Map;

public interface AuditRecordService {

    IotAuditLog create(String deviceId, String operator, String operationType, String remark,
                       Integer onChain, String txHash, Map<String, Object> detail, LocalDateTime operationTime);

    void markOnChainSuccess(String logId, String txHash);
}

