package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fabriciot.assembler.AuditAssembler;
import com.fabriciot.common.enums.OperationType;
import com.fabriciot.dto.audit.AuditListQuery;
import com.fabriciot.entity.IotAuditLog;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.mapper.IotAuditLogMapper;
import com.fabriciot.service.AuditService;
import com.fabriciot.vo.audit.AuditLogVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AuditServiceImpl implements AuditService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IotAuditLogMapper iotAuditLogMapper;
    private final AuditAssembler auditAssembler;

    public AuditServiceImpl(IotAuditLogMapper iotAuditLogMapper, AuditAssembler auditAssembler) {
        this.iotAuditLogMapper = iotAuditLogMapper;
        this.auditAssembler = auditAssembler;
    }

    @Override
    public IPage<AuditLogVO> list(AuditListQuery query) {
        LambdaQueryWrapper<IotAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotAuditLog::getDeleted, 0)
                .like(StringUtils.isNotBlank(query.getDeviceId()), IotAuditLog::getDeviceId, query.getDeviceId())
                .like(StringUtils.isNotBlank(query.getOperator()), IotAuditLog::getOperator, query.getOperator());
        if (StringUtils.isNotBlank(query.getOperationType())) {
            OperationType type = OperationType.fromCodeOrLabel(query.getOperationType());
            wrapper.eq(IotAuditLog::getOperationType, type.getCode());
        }
        if (StringUtils.isNotBlank(query.getStartTime()) && StringUtils.isNotBlank(query.getEndTime())) {
            LocalDateTime start = LocalDateTime.parse(query.getStartTime(), DATETIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(query.getEndTime(), DATETIME_FORMATTER);
            wrapper.between(IotAuditLog::getOperationTime, start, end);
        }
        wrapper.orderByDesc(IotAuditLog::getOperationTime);
        Page<IotAuditLog> page = iotAuditLogMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        Page<AuditLogVO> resultPage = new Page<>(query.getPage(), query.getPageSize(), page.getTotal());
        resultPage.setRecords(page.getRecords().stream().map(auditAssembler::toVO).toList());
        return resultPage;
    }

    @Override
    public AuditLogVO detail(String logId) {
        IotAuditLog entity = iotAuditLogMapper.selectByLogId(logId);
        if (entity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "审计日志不存在: " + logId);
        }
        return auditAssembler.toVO(entity);
    }
}
