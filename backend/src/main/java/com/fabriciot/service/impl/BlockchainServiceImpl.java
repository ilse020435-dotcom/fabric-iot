package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fabriciot.assembler.AssemblerSupport;
import com.fabriciot.assembler.BlockchainAssembler;
import com.fabriciot.common.enums.ChainStatus;
import com.fabriciot.dto.blockchain.BlockchainListQuery;
import com.fabriciot.entity.IotAuditLog;
import com.fabriciot.entity.IotBlockchainTx;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.mapper.IotAuditLogMapper;
import com.fabriciot.mapper.IotBlockchainTxMapper;
import com.fabriciot.service.BlockchainService;
import com.fabriciot.vo.blockchain.BlockchainDetailVO;
import com.fabriciot.vo.blockchain.BlockchainRecordVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BlockchainServiceImpl implements BlockchainService {

    private final IotBlockchainTxMapper iotBlockchainTxMapper;
    private final IotAuditLogMapper iotAuditLogMapper;
    private final BlockchainAssembler blockchainAssembler;
    private final AssemblerSupport assemblerSupport;

    public BlockchainServiceImpl(IotBlockchainTxMapper iotBlockchainTxMapper, IotAuditLogMapper iotAuditLogMapper,
                                 BlockchainAssembler blockchainAssembler, AssemblerSupport assemblerSupport) {
        this.iotBlockchainTxMapper = iotBlockchainTxMapper;
        this.iotAuditLogMapper = iotAuditLogMapper;
        this.blockchainAssembler = blockchainAssembler;
        this.assemblerSupport = assemblerSupport;
    }

    @Override
    public IPage<BlockchainRecordVO> list(BlockchainListQuery query) {
        LambdaQueryWrapper<IotBlockchainTx> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotBlockchainTx::getDeleted, 0)
                .like(StringUtils.isNotBlank(query.getDeviceId()), IotBlockchainTx::getDeviceId, query.getDeviceId());
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(IotBlockchainTx::getChainStatus, ChainStatus.fromCodeOrLabel(query.getStatus()).getCode());
        }
        wrapper.orderByDesc(IotBlockchainTx::getTimestamp);
        Page<IotBlockchainTx> page = iotBlockchainTxMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        Page<BlockchainRecordVO> resultPage = new Page<>(query.getPage(), query.getPageSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream().map(blockchainAssembler::toRecordVO).toList());
        return resultPage;
    }

    @Override
    public BlockchainDetailVO detail(String txHash) {
        IotBlockchainTx entity = iotBlockchainTxMapper.selectByTxHash(txHash);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "区块链交易不存在: " + txHash);
        }
        BlockchainDetailVO detailVO = blockchainAssembler.toDetailVO(entity);
        IotAuditLog auditLog = iotAuditLogMapper.selectByTxHash(txHash);
        if (auditLog != null) {
            Map<String, Object> payload = new LinkedHashMap<>(detailVO.getPayload() == null ? Map.of() : detailVO.getPayload());
            payload.putIfAbsent("operator", auditLog.getOperator());
            payload.putIfAbsent("operateTime", assemblerSupport.format(auditLog.getOperationTime()));
            detailVO.setPayload(payload);
        }
        return detailVO;
    }
}
