package com.fabriciot.assembler;

import com.fabriciot.entity.IotBlockchainTx;
import com.fabriciot.vo.blockchain.BlockchainDetailVO;
import com.fabriciot.vo.blockchain.BlockchainRecordVO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BlockchainAssembler {

    private final AssemblerSupport support;

    public BlockchainAssembler(AssemblerSupport support) {
        this.support = support;
    }

    public BlockchainRecordVO toRecordVO(IotBlockchainTx entity) {
        BlockchainRecordVO vo = new BlockchainRecordVO();
        vo.setBlockHeight(entity.getBlockHeight());
        vo.setTxHash(entity.getTxHash());
        vo.setDeviceId(entity.getDeviceId());
        vo.setOperationType(support.operationLabel(entity.getOperationType()));
        vo.setTimestamp(support.format(entity.getTimestamp()));
        vo.setChainStatus(support.chainLabel(entity.getChainStatus()));
        vo.setSummaryHash(entity.getSummaryHash());
        return vo;
    }

    public BlockchainDetailVO toDetailVO(IotBlockchainTx entity) {
        BlockchainDetailVO vo = new BlockchainDetailVO();
        vo.setBlockHeight(entity.getBlockHeight());
        vo.setTxHash(entity.getTxHash());
        vo.setChannelName(entity.getChannelName());
        vo.setContractName(entity.getContractName());
        vo.setWriteSet(entity.getWriteSetJson() == null ? Map.of() : entity.getWriteSetJson());
        vo.setSummaryHash(entity.getSummaryHash());
        vo.setPayload(entity.getPayloadJson() == null ? Map.of() : entity.getPayloadJson());
        vo.setTimestamp(support.format(entity.getTimestamp()));
        vo.setChainStatus(support.chainLabel(entity.getChainStatus()));
        return vo;
    }
}

