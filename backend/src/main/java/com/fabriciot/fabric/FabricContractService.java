package com.fabriciot.fabric;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FabricContractService {

    private final FabricGatewayClient fabricGatewayClient;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        String nowText = LocalDateTime.now().format(TIME_FORMATTER);
        // Current business flow has no explicit on-chain owner field source, keep backward compatibility by using operator.
        return submitTransaction("CreateDevice", deviceId, deviceName, operator, operator, nowText);
    }

    public FabricTransactionResult changeLifecycle(String deviceId, String beforeStatus, String afterStatus, String summaryHash, String operator) {
        return submitTransaction("UpdateStatus", deviceId, afterStatus, operator, LocalDateTime.now().format(TIME_FORMATTER));
    }

    public FabricTransactionResult syncStatusSummary(String deviceId, String monitorStatus, String summaryHash, String operator) {
        return submitTransaction("UpdateStatus", deviceId, monitorStatus, operator, LocalDateTime.now().format(TIME_FORMATTER));
    }

    public String queryChainRecord(String txHash) {
        return evaluateTransaction("queryChainRecord", txHash);
    }
}

