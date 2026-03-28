package com.fabriciot.fabric;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FabricTransactionResult {

    private boolean success;
    private String txHash;
    private Long blockHeight;
    private String responsePayload;
    private Map<String, Object> writeSet;
    private String message;
}

