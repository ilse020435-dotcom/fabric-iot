package com.fabriciot.service.impl;

import com.fabriciot.common.enums.ChainStatus;
import com.fabriciot.entity.IotBlockchainTx;
import com.fabriciot.fabric.FabricProperties;
import com.fabriciot.fabric.FabricTransactionResult;
import com.fabriciot.mapper.IotBlockchainTxMapper;
import com.fabriciot.service.BlockchainTxRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class BlockchainTxRecordServiceImpl implements BlockchainTxRecordService {

    private final IotBlockchainTxMapper iotBlockchainTxMapper;
    private final FabricProperties fabricProperties;

    public BlockchainTxRecordServiceImpl(IotBlockchainTxMapper iotBlockchainTxMapper, FabricProperties fabricProperties) {
        this.iotBlockchainTxMapper = iotBlockchainTxMapper;
        this.fabricProperties = fabricProperties;
    }

    @Override
    public void recordSuccess(String deviceId, String operationType, String summaryHash, FabricTransactionResult txResult,
                              Map<String, Object> payloadJson) {
        if (iotBlockchainTxMapper.selectByTxHash(txResult.getTxHash()) != null) {
            return;
        }
        IotBlockchainTx entity = new IotBlockchainTx();
        entity.setTxHash(txResult.getTxHash());
        entity.setBlockHeight(txResult.getBlockHeight() == null ? 0L : txResult.getBlockHeight());
        entity.setDeviceId(deviceId);
        entity.setOperationType(operationType);
        entity.setChannelName(fabricProperties.getChannelName());
        entity.setContractName(fabricProperties.getContractName());
        entity.setSummaryHash(summaryHash);
        entity.setChainStatus(ChainStatus.SUCCESS.getCode());
        entity.setPayloadJson(payloadJson);
        entity.setWriteSetJson(txResult.getWriteSet());
        entity.setDeleted(0);
        entity.setTimestamp(LocalDateTime.now());
        iotBlockchainTxMapper.insert(entity);
    }

    @Override
    public void recordFailed(String deviceId, String operationType, String summaryHash, String txHash,
                             String reason, Map<String, Object> payloadJson) {
        if (iotBlockchainTxMapper.selectByTxHash(txHash) != null) {
            return;
        }
        IotBlockchainTx entity = new IotBlockchainTx();
        entity.setTxHash(txHash);
        entity.setBlockHeight(0L);
        entity.setDeviceId(deviceId);
        entity.setOperationType(operationType);
        entity.setChannelName(fabricProperties.getChannelName());
        entity.setContractName(fabricProperties.getContractName());
        entity.setSummaryHash(summaryHash);
        entity.setChainStatus(ChainStatus.FAILED.getCode());
        entity.setPayloadJson(payloadJson);
        Map<String, Object> writeSet = new HashMap<>();
        writeSet.put("reason", reason);
        entity.setWriteSetJson(writeSet);
        entity.setDeleted(0);
        entity.setTimestamp(LocalDateTime.now());
        iotBlockchainTxMapper.insert(entity);
    }
}

