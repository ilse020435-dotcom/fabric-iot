package com.fabriciot.service;

import com.fabriciot.fabric.FabricTransactionResult;

import java.util.Map;

public interface BlockchainTxRecordService {

    void recordSuccess(String deviceId, String operationType, String summaryHash, FabricTransactionResult txResult,
                       Map<String, Object> payloadJson);

    void recordFailed(String deviceId, String operationType, String summaryHash, String txHash,
                      String reason, Map<String, Object> payloadJson);
}

