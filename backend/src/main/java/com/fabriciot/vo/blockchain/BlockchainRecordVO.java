package com.fabriciot.vo.blockchain;

import lombok.Data;

@Data
public class BlockchainRecordVO {

    private Long blockHeight;
    private String txHash;
    private String deviceId;
    private String operationType;
    private String timestamp;
    private String chainStatus;
    private String summaryHash;
}

