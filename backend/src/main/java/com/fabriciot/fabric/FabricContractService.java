package com.fabriciot.fabric;

import org.springframework.stereotype.Service;

@Service
public class FabricContractService {

    private final FabricGatewayClient fabricGatewayClient;

    public FabricContractService(FabricGatewayClient fabricGatewayClient) {
        this.fabricGatewayClient = fabricGatewayClient;
    }

    public FabricTransactionResult submitTransaction(String function, String... args) {
        return fabricGatewayClient.submitTransaction(function, args);
    }

    public String evaluateTransaction(String function, String... args) {
        return fabricGatewayClient.evaluateTransaction(function, args);
    }

    public FabricTransactionResult registerDevice(String deviceId, String deviceName, String operator) {
        return submitTransaction("CreateDevice", deviceId, deviceName, operator);
    }

    // The current iot-java contract exposes only UpdateStatus(deviceId, status).
    public FabricTransactionResult changeLifecycle(String deviceId, String beforeStatus, String afterStatus, String summaryHash, String operator) {
        return submitTransaction("UpdateStatus", deviceId, afterStatus);
    }

    public FabricTransactionResult syncStatusSummary(String deviceId, String monitorStatus, String summaryHash, String operator) {
        return submitTransaction("UpdateStatus", deviceId, monitorStatus);
    }

    public String queryChainRecord(String txHash) {
        return evaluateTransaction("queryChainRecord", txHash);
    }
}

