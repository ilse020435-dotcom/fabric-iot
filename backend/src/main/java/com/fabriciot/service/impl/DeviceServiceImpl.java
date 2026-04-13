package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fabriciot.assembler.AssemblerSupport;
import com.fabriciot.assembler.DeviceAssembler;
import com.fabriciot.common.enums.ChainStatus;
import com.fabriciot.common.enums.LifecycleStatus;
import com.fabriciot.common.enums.MonitorStatus;
import com.fabriciot.common.enums.OperationType;
import com.fabriciot.common.util.HashUtil;
import com.fabriciot.common.util.IdGeneratorUtil;
import com.fabriciot.dto.device.DeviceListQuery;
import com.fabriciot.dto.device.DeviceSaveRequest;
import com.fabriciot.dto.device.DeviceStatusReportRequest;
import com.fabriciot.entity.IotAuditLog;
import com.fabriciot.entity.IotBlockchainTx;
import com.fabriciot.entity.IotDevice;
import com.fabriciot.entity.IotDeviceLifecycleEvent;
import com.fabriciot.entity.IotDeviceStatusSnapshot;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.fabric.FabricContractService;
import com.fabriciot.fabric.FabricTransactionResult;
import com.fabriciot.mapper.IotAuditLogMapper;
import com.fabriciot.mapper.IotBlockchainTxMapper;
import com.fabriciot.mapper.IotDeviceLifecycleEventMapper;
import com.fabriciot.mapper.IotDeviceMapper;
import com.fabriciot.mapper.IotDeviceStatusSnapshotMapper;
import com.fabriciot.service.AuditRecordService;
import com.fabriciot.service.BlockchainTxRecordService;
import com.fabriciot.service.DeviceService;
import com.fabriciot.vo.device.DeviceDetailVO;
import com.fabriciot.vo.device.DeviceListVO;
import com.fabriciot.vo.device.LifecycleEventVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    private static final Map<LifecycleStatus, Set<LifecycleStatus>> STATUS_TRANSITIONS = new EnumMap<>(LifecycleStatus.class);

    static {
        STATUS_TRANSITIONS.put(LifecycleStatus.REGISTERED, Set.of(LifecycleStatus.ACTIVATED, LifecycleStatus.REVOKED));
        STATUS_TRANSITIONS.put(LifecycleStatus.ACTIVATED, Set.of(LifecycleStatus.FROZEN, LifecycleStatus.REVOKED));
        STATUS_TRANSITIONS.put(LifecycleStatus.FROZEN, Set.of(LifecycleStatus.ACTIVATED, LifecycleStatus.REVOKED));
        STATUS_TRANSITIONS.put(LifecycleStatus.REVOKED, Set.of());
    }

    private final IotDeviceMapper iotDeviceMapper;
    private final IotDeviceLifecycleEventMapper lifecycleEventMapper;
    private final IotDeviceStatusSnapshotMapper snapshotMapper;
    private final IotAuditLogMapper auditLogMapper;
    private final IotBlockchainTxMapper blockchainTxMapper;
    private final DeviceAssembler deviceAssembler;
    private final AssemblerSupport assemblerSupport;
    private final FabricContractService fabricContractService;
    private final AuditRecordService auditRecordService;
    private final BlockchainTxRecordService blockchainTxRecordService;

    public DeviceServiceImpl(IotDeviceMapper iotDeviceMapper, IotDeviceLifecycleEventMapper lifecycleEventMapper,
                             IotDeviceStatusSnapshotMapper snapshotMapper, IotAuditLogMapper auditLogMapper,
                             IotBlockchainTxMapper blockchainTxMapper, DeviceAssembler deviceAssembler,
                             AssemblerSupport assemblerSupport, FabricContractService fabricContractService,
                             AuditRecordService auditRecordService, BlockchainTxRecordService blockchainTxRecordService) {
        this.iotDeviceMapper = iotDeviceMapper;
        this.lifecycleEventMapper = lifecycleEventMapper;
        this.snapshotMapper = snapshotMapper;
        this.auditLogMapper = auditLogMapper;
        this.blockchainTxMapper = blockchainTxMapper;
        this.deviceAssembler = deviceAssembler;
        this.assemblerSupport = assemblerSupport;
        this.fabricContractService = fabricContractService;
        this.auditRecordService = auditRecordService;
        this.blockchainTxRecordService = blockchainTxRecordService;
    }

    @Override
    public IPage<DeviceListVO> list(DeviceListQuery query) {
        LambdaQueryWrapper<IotDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotDevice::getDeleted, 0)
                .like(StringUtils.isNotBlank(query.getDeviceId()), IotDevice::getDeviceId, query.getDeviceId())
                .like(StringUtils.isNotBlank(query.getDeviceName()), IotDevice::getDeviceName, query.getDeviceName());
        if (StringUtils.isNotBlank(query.getStatus())) {
            LifecycleStatus status = LifecycleStatus.fromCodeOrLabel(query.getStatus());
            wrapper.eq(IotDevice::getLifecycleStatus, status.getCode());
        }
        wrapper.orderByDesc(IotDevice::getRegisterTime);
        Page<IotDevice> page = iotDeviceMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        Page<DeviceListVO> result = new Page<>(query.getPage(), query.getPageSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(deviceAssembler::toListVO).toList());
        return result;
    }

    @Override
    public DeviceDetailVO detail(String deviceId) {
        IotDevice device = requireDevice(deviceId);

        DeviceDetailVO detailVO = new DeviceDetailVO();
        detailVO.setBasic(deviceAssembler.toListVO(device));

        List<DeviceDetailVO.StatusRecordVO> statusRecords = snapshotMapper.selectRecentByDeviceId(deviceId, 20).stream()
                .map(deviceAssembler::toStatusRecordVO)
                .toList();
        detailVO.setStatusRecords(statusRecords);

        DeviceDetailVO.ChainSummaryVO chainSummaryVO = new DeviceDetailVO.ChainSummaryVO();
        IotBlockchainTx latestTx = findLatestBlockchainTx(deviceId);
        if (latestTx == null) {
            chainSummaryVO.setLatestBlockHeight(0L);
            chainSummaryVO.setLatestTxHash("--");
            chainSummaryVO.setChainHash("--");
            chainSummaryVO.setSyncedAt("");
        } else {
            chainSummaryVO.setLatestBlockHeight(latestTx.getBlockHeight());
            chainSummaryVO.setLatestTxHash(latestTx.getTxHash());
            chainSummaryVO.setChainHash(latestTx.getSummaryHash());
            chainSummaryVO.setSyncedAt(assemblerSupport.format(latestTx.getTimestamp()));
        }
        detailVO.setChainSummary(chainSummaryVO);

        List<DeviceDetailVO.RecentAuditVO> recentAudits = auditLogMapper.selectList(
                        new LambdaQueryWrapper<IotAuditLog>()
                                .eq(IotAuditLog::getDeleted, 0)
                                .eq(IotAuditLog::getDeviceId, deviceId)
                                .orderByDesc(IotAuditLog::getOperationTime)
                                .last("LIMIT 6"))
                .stream()
                .map(item -> {
                    DeviceDetailVO.RecentAuditVO vo = new DeviceDetailVO.RecentAuditVO();
                    vo.setLogId(item.getLogId());
                    vo.setOperationType(assemblerSupport.operationLabel(item.getOperationType()));
                    vo.setOperator(item.getOperator());
                    vo.setTimestamp(assemblerSupport.format(item.getOperationTime()));
                    vo.setRemark(item.getRemark());
                    return vo;
                })
                .toList();
        detailVO.setRecentAudits(recentAudits);
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceListVO create(DeviceSaveRequest request, String operator) {
        String deviceId = StringUtils.isBlank(request.getDeviceId()) ? IdGeneratorUtil.nextDeviceId() : request.getDeviceId();
        String did = StringUtils.isBlank(request.getDid()) ? IdGeneratorUtil.nextDid(deviceId) : request.getDid();
        if (iotDeviceMapper.selectByDeviceId(deviceId) != null) {
            throw new BizException(ErrorCode.BIZ_ERROR, "device_id 已存在");
        }
        if (iotDeviceMapper.selectByDid(did) != null) {
            throw new BizException(ErrorCode.BIZ_ERROR, "did 已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        IotDevice entity = new IotDevice();
        entity.setDeviceId(deviceId);
        entity.setDeviceName(request.getDeviceName());
        entity.setDid(did);
        entity.setDeviceType(request.getDeviceType());
        entity.setVendor(request.getVendor());
        entity.setLifecycleStatus(LifecycleStatus.REGISTERED.getCode());
        entity.setDescription(request.getDescription());
        entity.setDeleted(0);
        entity.setRegisterTime(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        iotDeviceMapper.insert(entity);

        String summaryHash = HashUtil.sha256Hex(deviceId + "|" + did + "|" + entity.getLifecycleStatus() + "|" + now);
        IotDeviceLifecycleEvent lifecycleEvent = new IotDeviceLifecycleEvent();
        lifecycleEvent.setDeviceId(deviceId);
        lifecycleEvent.setOperationType(OperationType.REGISTER.getCode());
        lifecycleEvent.setBeforeStatus(null);
        lifecycleEvent.setAfterStatus(LifecycleStatus.REGISTERED.getCode());
        lifecycleEvent.setSummaryHash(summaryHash);
        lifecycleEvent.setOperator(operator);
        lifecycleEvent.setOccurredAt(now);
        lifecycleEvent.setDeleted(0);
        lifecycleEventMapper.insert(lifecycleEvent);

        Map<String, Object> detail = new HashMap<>();
        detail.put("deviceId", deviceId);
        detail.put("did", did);
        detail.put("summaryHash", summaryHash);
        detail.put("request", request);
        IotAuditLog auditLog = auditRecordService.create(deviceId, operator, OperationType.REGISTER.getCode(),
                "注册设备并写入链上身份", 0, null, detail, now);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("did", did);
        payload.put("operationType", OperationType.REGISTER.getCode());
        payload.put("summaryHash", summaryHash);
        attachOperationMeta(payload, operator, now);

        try {
            FabricTransactionResult txResult = fabricContractService.registerDevice(deviceId, request.getDeviceName(), operator);
            if (txResult.isSuccess()) {
                lifecycleEventMapper.update(null, new LambdaUpdateWrapper<IotDeviceLifecycleEvent>()
                        .eq(IotDeviceLifecycleEvent::getId, lifecycleEvent.getId())
                        .set(IotDeviceLifecycleEvent::getTxHash, txResult.getTxHash()));
                auditRecordService.markOnChainSuccess(auditLog.getLogId(), txResult.getTxHash());
                blockchainTxRecordService.recordSuccess(deviceId, OperationType.REGISTER.getCode(), summaryHash, txResult, payload);
            } else {
                String failedTx = StringUtils.defaultIfBlank(txResult.getTxHash(),
                        IdGeneratorUtil.fallbackTxHash("REGISTER:" + deviceId));
                blockchainTxRecordService.recordFailed(deviceId, OperationType.REGISTER.getCode(), summaryHash, failedTx,
                        txResult.getMessage(), payload);
            }
        } catch (Exception ex) {
            String failedTx = IdGeneratorUtil.fallbackTxHash("REGISTER:" + deviceId);
            blockchainTxRecordService.recordFailed(deviceId, OperationType.REGISTER.getCode(), summaryHash, failedTx,
                    ex.getMessage(), payload);
            log.warn("Fabric register failed, deviceId={}, reason={}", deviceId, ex.getMessage());
        }
        return deviceAssembler.toListVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceListVO update(String deviceId, DeviceSaveRequest request, String operator) {
        IotDevice entity = requireDevice(deviceId);
        entity.setDeviceName(request.getDeviceName());
        entity.setDeviceType(request.getDeviceType());
        entity.setVendor(request.getVendor());
        entity.setDescription(request.getDescription());
        entity.setUpdatedAt(LocalDateTime.now());
        iotDeviceMapper.updateById(entity);

        String summaryHash = HashUtil.sha256Hex(deviceId + "|" + request.getDeviceName() + "|" + request.getDeviceType()
                + "|" + request.getVendor() + "|" + entity.getUpdatedAt());
        IotDeviceLifecycleEvent lifecycleEvent = new IotDeviceLifecycleEvent();
        lifecycleEvent.setDeviceId(deviceId);
        lifecycleEvent.setOperationType(OperationType.UPDATE.getCode());
        lifecycleEvent.setBeforeStatus(entity.getLifecycleStatus());
        lifecycleEvent.setAfterStatus(entity.getLifecycleStatus());
        lifecycleEvent.setSummaryHash(summaryHash);
        lifecycleEvent.setOperator(operator);
        lifecycleEvent.setOccurredAt(LocalDateTime.now());
        lifecycleEvent.setDeleted(0);
        lifecycleEventMapper.insert(lifecycleEvent);

        Map<String, Object> detail = new HashMap<>();
        detail.put("summaryHash", summaryHash);
        detail.put("request", request);
        IotAuditLog auditLog = auditRecordService.create(deviceId, operator, OperationType.UPDATE.getCode(),
                "设备元数据变更并同步链上摘要", 0, null, detail, LocalDateTime.now());

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("operationType", OperationType.UPDATE.getCode());
        payload.put("summaryHash", summaryHash);
        payload.put("request", request);
        attachOperationMeta(payload, operator, lifecycleEvent.getOccurredAt());
        try {
            FabricTransactionResult txResult = fabricContractService.submitTransaction("updateDeviceMetadata", deviceId, summaryHash, operator);
            if (txResult.isSuccess()) {
                lifecycleEventMapper.update(null, new LambdaUpdateWrapper<IotDeviceLifecycleEvent>()
                        .eq(IotDeviceLifecycleEvent::getId, lifecycleEvent.getId())
                        .set(IotDeviceLifecycleEvent::getTxHash, txResult.getTxHash()));
                auditRecordService.markOnChainSuccess(auditLog.getLogId(), txResult.getTxHash());
                blockchainTxRecordService.recordSuccess(deviceId, OperationType.UPDATE.getCode(), summaryHash, txResult, payload);
            } else {
                String failedTx = StringUtils.defaultIfBlank(txResult.getTxHash(),
                        IdGeneratorUtil.fallbackTxHash("UPDATE:" + deviceId));
                blockchainTxRecordService.recordFailed(deviceId, OperationType.UPDATE.getCode(), summaryHash, failedTx,
                        txResult.getMessage(), payload);
            }
        } catch (Exception ex) {
            String failedTx = IdGeneratorUtil.fallbackTxHash("UPDATE:" + deviceId);
            blockchainTxRecordService.recordFailed(deviceId, OperationType.UPDATE.getCode(), summaryHash, failedTx,
                    ex.getMessage(), payload);
            log.warn("Fabric update failed, deviceId={}, reason={}", deviceId, ex.getMessage());
        }
        return deviceAssembler.toListVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceListVO changeStatus(String deviceId, String lifecycleStatus, String operator) {
        IotDevice entity = requireDevice(deviceId);
        LifecycleStatus current = LifecycleStatus.fromCodeOrLabel(entity.getLifecycleStatus());
        LifecycleStatus target = LifecycleStatus.fromCodeOrLabel(lifecycleStatus);
        if (current == target) {
            return deviceAssembler.toListVO(entity);
        }
        Set<LifecycleStatus> nextStatusSet = STATUS_TRANSITIONS.getOrDefault(current, Set.of());
        if (!nextStatusSet.contains(target)) {
            throw new BizException(ErrorCode.BIZ_ERROR, "非法状态流转: " + current.getCode() + " -> " + target.getCode());
        }
        entity.setLifecycleStatus(target.getCode());
        entity.setUpdatedAt(LocalDateTime.now());
        iotDeviceMapper.updateById(entity);

        OperationType operationType = switch (target) {
            case ACTIVATED -> OperationType.ACTIVATE;
            case FROZEN -> OperationType.FREEZE;
            case REVOKED -> OperationType.REVOKE;
            default -> OperationType.UPDATE;
        };
        String summaryHash = HashUtil.sha256Hex(deviceId + "|" + current.getCode() + "|" + target.getCode() + "|" + LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        IotDeviceLifecycleEvent lifecycleEvent = new IotDeviceLifecycleEvent();
        lifecycleEvent.setDeviceId(deviceId);
        lifecycleEvent.setOperationType(operationType.getCode());
        lifecycleEvent.setBeforeStatus(current.getCode());
        lifecycleEvent.setAfterStatus(target.getCode());
        lifecycleEvent.setSummaryHash(summaryHash);
        lifecycleEvent.setOperator(operator);
        lifecycleEvent.setOccurredAt(now);
        lifecycleEvent.setDeleted(0);
        lifecycleEventMapper.insert(lifecycleEvent);

        MonitorStatus monitorStatus = mapLifecycleToMonitorStatus(target);
        IotDeviceStatusSnapshot snapshot = null;
        if (monitorStatus != null) {
            snapshot = new IotDeviceStatusSnapshot();
            snapshot.setDeviceId(deviceId);
            snapshot.setMonitorStatus(monitorStatus.getCode());
            snapshot.setSummaryHash(summaryHash);
            snapshot.setDeleted(0);
            snapshot.setUpdatedAt(now);
            snapshotMapper.insert(snapshot);
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("beforeStatus", current.getCode());
        detail.put("afterStatus", target.getCode());
        detail.put("summaryHash", summaryHash);
        IotAuditLog auditLog = auditRecordService.create(deviceId, operator, operationType.getCode(),
                "设备状态更新为" + target.getLabel(), 0, null, detail, now);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("operationType", operationType.getCode());
        payload.put("beforeStatus", current.getCode());
        payload.put("afterStatus", target.getCode());
        payload.put("summaryHash", summaryHash);
        attachOperationMeta(payload, operator, now);
        try {
            FabricTransactionResult txResult = fabricContractService.changeLifecycle(deviceId, current.getCode(), target.getCode(), summaryHash, operator);
            if (txResult.isSuccess()) {
                lifecycleEventMapper.update(null, new LambdaUpdateWrapper<IotDeviceLifecycleEvent>()
                        .eq(IotDeviceLifecycleEvent::getId, lifecycleEvent.getId())
                        .set(IotDeviceLifecycleEvent::getTxHash, txResult.getTxHash()));
                if (snapshot != null && snapshot.getId() != null) {
                    snapshotMapper.update(null, new LambdaUpdateWrapper<IotDeviceStatusSnapshot>()
                            .eq(IotDeviceStatusSnapshot::getId, snapshot.getId())
                            .set(IotDeviceStatusSnapshot::getTxHash, txResult.getTxHash())
                            .set(IotDeviceStatusSnapshot::getBlockHeight,
                                    txResult.getBlockHeight() == null ? 0L : txResult.getBlockHeight()));
                }
                auditRecordService.markOnChainSuccess(auditLog.getLogId(), txResult.getTxHash());
                blockchainTxRecordService.recordSuccess(deviceId, operationType.getCode(), summaryHash, txResult, payload);
            } else {
                String failedTx = StringUtils.defaultIfBlank(txResult.getTxHash(),
                        IdGeneratorUtil.fallbackTxHash(operationType.getCode() + ":" + deviceId));
                blockchainTxRecordService.recordFailed(deviceId, operationType.getCode(), summaryHash, failedTx,
                        txResult.getMessage(), payload);
            }
        } catch (Exception ex) {
            String failedTx = IdGeneratorUtil.fallbackTxHash(operationType.getCode() + ":" + deviceId);
            blockchainTxRecordService.recordFailed(deviceId, operationType.getCode(), summaryHash, failedTx,
                    ex.getMessage(), payload);
            log.warn("Fabric lifecycle change failed, deviceId={}, reason={}", deviceId, ex.getMessage());
        }
        return deviceAssembler.toListVO(entity);
    }

    @Override
    public DeviceListVO freeze(String deviceId, String operator) {
        return changeStatus(deviceId, LifecycleStatus.FROZEN.getCode(), operator);
    }

    @Override
    public DeviceListVO revoke(String deviceId, String operator) {
        return changeStatus(deviceId, LifecycleStatus.REVOKED.getCode(), operator);
    }

    @Override
    public DeviceListVO activate(String deviceId, String operator) {
        return changeStatus(deviceId, LifecycleStatus.ACTIVATED.getCode(), operator);
    }

    @Override
    public List<LifecycleEventVO> lifecycleRecords(String deviceId, Integer limit) {
        requireDevice(deviceId);
        int size = limit == null || limit < 1 ? 20 : Math.min(limit, 200);
        return lifecycleEventMapper.selectRecentByDeviceId(deviceId, size)
                .stream()
                .map(deviceAssembler::toLifecycleVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportStatus(String deviceId, DeviceStatusReportRequest request, String operator) {
        requireDevice(deviceId);
        MonitorStatus monitorStatus = MonitorStatus.fromCodeOrLabel(request.getMonitorStatus());
        LocalDateTime now = LocalDateTime.now();
        String summaryHash = HashUtil.sha256Hex(deviceId + "|" + monitorStatus.getCode() + "|"
                + StringUtils.defaultString(request.getSignalStrength()) + "|"
                + StringUtils.defaultString(request.getTemperature()) + "|" + now);

        IotDeviceStatusSnapshot snapshot = new IotDeviceStatusSnapshot();
        snapshot.setDeviceId(deviceId);
        snapshot.setMonitorStatus(monitorStatus.getCode());
        snapshot.setSignalStrength(request.getSignalStrength());
        snapshot.setTemperature(request.getTemperature());
        snapshot.setSummaryHash(summaryHash);
        snapshot.setDeleted(0);
        snapshot.setUpdatedAt(now);
        snapshotMapper.insert(snapshot);

        Map<String, Object> detail = new HashMap<>();
        detail.put("monitorStatus", monitorStatus.getCode());
        detail.put("signalStrength", request.getSignalStrength());
        detail.put("temperature", request.getTemperature());
        detail.put("summaryHash", summaryHash);
        IotAuditLog auditLog = auditRecordService.create(deviceId, operator, OperationType.SYNC_STATUS.getCode(),
                "同步设备状态摘要", 0, null, detail, now);

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", deviceId);
        payload.put("monitorStatus", monitorStatus.getCode());
        payload.put("signalStrength", request.getSignalStrength());
        payload.put("temperature", request.getTemperature());
        payload.put("summaryHash", summaryHash);
        attachOperationMeta(payload, operator, now);

        try {
            FabricTransactionResult txResult = fabricContractService.syncStatusSummary(deviceId, monitorStatus.getCode(), summaryHash, operator);
            if (txResult.isSuccess()) {
                snapshotMapper.update(null, new LambdaUpdateWrapper<IotDeviceStatusSnapshot>()
                        .eq(IotDeviceStatusSnapshot::getId, snapshot.getId())
                        .set(IotDeviceStatusSnapshot::getTxHash, txResult.getTxHash())
                        .set(IotDeviceStatusSnapshot::getBlockHeight, txResult.getBlockHeight()));
                auditRecordService.markOnChainSuccess(auditLog.getLogId(), txResult.getTxHash());
                blockchainTxRecordService.recordSuccess(deviceId, OperationType.SYNC_STATUS.getCode(), summaryHash, txResult, payload);
            } else {
                String failedTx = StringUtils.defaultIfBlank(txResult.getTxHash(),
                        IdGeneratorUtil.fallbackTxHash("SYNC_STATUS:" + deviceId));
                blockchainTxRecordService.recordFailed(deviceId, OperationType.SYNC_STATUS.getCode(), summaryHash, failedTx,
                        txResult.getMessage(), payload);
            }
        } catch (Exception ex) {
            String failedTx = IdGeneratorUtil.fallbackTxHash("SYNC_STATUS:" + deviceId);
            blockchainTxRecordService.recordFailed(deviceId, OperationType.SYNC_STATUS.getCode(), summaryHash, failedTx,
                    ex.getMessage(), payload);
            log.warn("Fabric status sync failed, deviceId={}, reason={}", deviceId, ex.getMessage());
        }
    }

    private IotDevice requireDevice(String deviceId) {
        IotDevice device = iotDeviceMapper.selectByDeviceId(deviceId);
        if (device == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "设备不存在: " + deviceId);
        }
        return device;
    }

    private MonitorStatus mapLifecycleToMonitorStatus(LifecycleStatus lifecycleStatus) {
        return switch (lifecycleStatus) {
            case ACTIVATED -> MonitorStatus.ONLINE;
            case FROZEN -> MonitorStatus.EXCEPTION;
            case REVOKED -> MonitorStatus.OFFLINE;
            default -> null;
        };
    }

    private IotBlockchainTx findLatestBlockchainTx(String deviceId) {
        List<IotBlockchainTx> list = blockchainTxMapper.selectList(new LambdaQueryWrapper<IotBlockchainTx>()
                .eq(IotBlockchainTx::getDeleted, 0)
                .eq(IotBlockchainTx::getDeviceId, deviceId)
                .eq(IotBlockchainTx::getChainStatus, ChainStatus.SUCCESS.getCode())
                .orderByDesc(IotBlockchainTx::getTimestamp)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private void attachOperationMeta(Map<String, Object> payload, String operator, LocalDateTime operateTime) {
        payload.put("operator", operator);
        payload.put("operateTime", assemblerSupport.format(operateTime));
    }
}
