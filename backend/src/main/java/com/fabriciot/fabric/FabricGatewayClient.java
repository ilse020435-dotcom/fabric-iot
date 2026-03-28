package com.fabriciot.fabric;

public interface FabricGatewayClient {

    FabricTransactionResult submitTransaction(String function, String... args);

    String evaluateTransaction(String function, String... args);
}

