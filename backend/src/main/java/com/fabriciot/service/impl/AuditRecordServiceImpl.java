package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fabriciot.common.util.IdGeneratorUtil;
import com.fabriciot.entity.IotAuditLog;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.mapper.IotAuditLogMapper;
import com.fabriciot.service.AuditRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditRecordServiceImpl implements AuditRecordService {

    private final IotAuditLogMapper iotAuditLogMapper;

    public AuditRecordServiceImpl(IotAuditLogMapper iotAuditLogMapper) {
        this.iotAuditLogMapper = iotAuditLogMapper;
    }

    @Override
    public IotAuditLog create(String deviceId, String operator, String operationType, String remark,
                              Integer onChain, String txHash, Map<String, Object> detail, LocalDateTime operationTime) {
        if (onChain != null && onChain == 1 && StringUtils.isBlank(txHash)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "on_chain=1 时 tx_hash 必填");
        }
        IotAuditLog log = new IotAuditLog();
        log.setLogId(IdGeneratorUtil.nextAuditLogId());
        log.setOperationTime(operationTime == null ? LocalDateTime.now() : operationTime);
        log.setOperator(operator);
        log.setOperationType(operationType);
        log.setDeviceId(deviceId);
        log.setOnChain(onChain == null ? 0 : onChain);
        log.setTxHash(txHash);
        log.setRemark(remark);
        log.setDetailJson(detail);
        log.setDeleted(0);
        log.setCreatedAt(LocalDateTime.now());
        iotAuditLogMapper.insert(log);
        return log;
    }

    @Override
    public void markOnChainSuccess(String logId, String txHash) {
        LambdaUpdateWrapper<IotAuditLog> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(IotAuditLog::getLogId, logId)
                .eq(IotAuditLog::getDeleted, 0)
                .set(IotAuditLog::getOnChain, 1)
                .set(IotAuditLog::getTxHash, txHash);
        iotAuditLogMapper.update(null, updateWrapper);
    }
}
