package com.fabriciot.assembler;

import com.fabriciot.entity.IotAuditLog;
import com.fabriciot.vo.audit.AuditLogVO;
import org.springframework.stereotype.Component;

@Component
public class AuditAssembler {

    private final AssemblerSupport support;

    public AuditAssembler(AssemblerSupport support) {
        this.support = support;
    }

    public AuditLogVO toVO(IotAuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setLogId(entity.getLogId());
        vo.setOperationTime(support.format(entity.getOperationTime()));
        vo.setOperator(entity.getOperator());
        vo.setOperationType(support.operationLabel(entity.getOperationType()));
        vo.setDeviceId(entity.getDeviceId());
        vo.setOnChain(support.onChainLabel(entity.getOnChain()));
        vo.setTxHash(entity.getTxHash() == null ? "--" : entity.getTxHash());
        vo.setRemark(entity.getRemark());
        vo.setDetail(entity.getDetailJson());
        return vo;
    }
}

