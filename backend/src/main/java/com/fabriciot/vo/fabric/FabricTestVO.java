package com.fabriciot.vo.fabric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FabricTestVO {

    private boolean enabled;
    private boolean mock;
    private boolean tlsEnabled;
    private boolean certConfigured;
    private boolean keyConfigured;
    private boolean tlsCertConfigured;
    private boolean gatewayConfigReady;
    private boolean tcpReachable;
    private boolean gatewayReady;
    private boolean chaincodeInvocationRequested;
    private boolean chaincodeInvocationSuccess;
    private boolean basicConnectionSuccess;
    private boolean endToEndSuccess;
    private boolean success;
    private String peerEndpoint;
    private String mspId;
    private String channelName;
    private String chaincodeName;
    private String contractName;
    private String function;
    private List<String> args;
    private String evaluateResult;
    private String message;
    private String error;
    private List<String> issues;
}
