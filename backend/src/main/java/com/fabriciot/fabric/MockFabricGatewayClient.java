package com.fabriciot.fabric;

import com.fabriciot.common.util.HashUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.fabric", name = "mock", havingValue = "true", matchIfMissing = true)
public class MockFabricGatewayClient implements FabricGatewayClient {

    @Override
    public FabricTransactionResult submitTransaction(String function, String... args) {
        String source = function + ":" + String.join("|", args) + ":" + System.nanoTime();
        String txHash = HashUtil.sha256Hex(source);
        long blockHeight = Math.abs(LocalDateTime.now().getNano() % 100000L) + 50000L;
        Map<String, Object> writeSet = new HashMap<>();
        writeSet.put("function", function);
        writeSet.put("args", args);
        writeSet.put("version", blockHeight + ":1");
        return FabricTransactionResult.builder()
                .success(true)
                .txHash(txHash)
                .blockHeight(blockHeight)
                .responsePayload("{\"mock\":true}")
                .writeSet(writeSet)
                .message("mock success")
                .build();
    }

    @Override
    public String evaluateTransaction(String function, String... args) {
        return "{\"mock\":true,\"function\":\"" + function + "\"}";
    }
}

