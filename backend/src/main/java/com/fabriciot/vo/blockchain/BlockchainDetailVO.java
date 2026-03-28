package com.fabriciot.vo.blockchain;

import lombok.Data;

import java.util.Map;

@Data
public class BlockchainDetailVO {

    private Long blockHeight;
    private String txHash;
    private String channelName;
    private String contractName;
    private Map<String, Object> writeSet;
    private String summaryHash;
    private Map<String, Object> payload;
    private String timestamp;
    private String chainStatus;
}

