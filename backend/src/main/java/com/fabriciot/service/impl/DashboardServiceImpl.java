package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fabriciot.assembler.AssemblerSupport;
import com.fabriciot.common.enums.LifecycleStatus;
import com.fabriciot.entity.IotAuditLog;
import com.fabriciot.entity.IotDevice;
import com.fabriciot.entity.IotDeviceStatusSnapshot;
import com.fabriciot.mapper.IotAuditLogMapper;
import com.fabriciot.mapper.IotDeviceMapper;
import com.fabriciot.mapper.IotDeviceStatusSnapshotMapper;
import com.fabriciot.service.DashboardService;
import com.fabriciot.vo.dashboard.DashboardOverviewVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final IotDeviceMapper iotDeviceMapper;
    private final IotDeviceStatusSnapshotMapper snapshotMapper;
    private final IotAuditLogMapper auditLogMapper;
    private final AssemblerSupport assemblerSupport;

    public DashboardServiceImpl(IotDeviceMapper iotDeviceMapper, IotDeviceStatusSnapshotMapper snapshotMapper,
                                IotAuditLogMapper auditLogMapper, AssemblerSupport assemblerSupport) {
        this.iotDeviceMapper = iotDeviceMapper;
        this.snapshotMapper = snapshotMapper;
        this.auditLogMapper = auditLogMapper;
        this.assemblerSupport = assemblerSupport;
    }

    @Override
    public DashboardOverviewVO getOverview() {
        List<IotDevice> devices = iotDeviceMapper.selectList(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0));
        Map<String, IotDeviceStatusSnapshot> latestStatusMap = devices.stream()
                .map(item -> new java.util.AbstractMap.SimpleEntry<>(item.getDeviceId(),
                        snapshotMapper.selectLatestByDeviceId(item.getDeviceId())))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        long totalDevices = devices.size();
        long onlineDevices = latestStatusMap.values().stream()
                .filter(item -> "ONLINE".equals(item.getMonitorStatus()))
                .count();
        long frozenDevices = devices.stream()
                .filter(item -> LifecycleStatus.FROZEN.getCode().equals(item.getLifecycleStatus()))
                .count();
        long deactivatedDevices = devices.stream()
                .filter(item -> LifecycleStatus.REVOKED.getCode().equals(item.getLifecycleStatus()))
                .count();
        LocalDate today = LocalDate.now();
        long newDevicesToday = devices.stream()
                .filter(item -> item.getRegisterTime() != null && item.getRegisterTime().toLocalDate().isEqual(today))
                .count();

        DashboardOverviewVO overviewVO = new DashboardOverviewVO();
        DashboardOverviewVO.StatsVO statsVO = new DashboardOverviewVO.StatsVO();
        statsVO.setTotalDevices(totalDevices);
        statsVO.setOnlineDevices(onlineDevices);
        statsVO.setFrozenDevices(frozenDevices);
        statsVO.setDeactivatedDevices(deactivatedDevices);
        statsVO.setNewDevicesToday(newDevicesToday);
        overviewVO.setStats(statsVO);

        List<DashboardOverviewVO.RecentOperationVO> recentOperations = auditLogMapper.selectList(
                        new LambdaQueryWrapper<IotAuditLog>()
                                .eq(IotAuditLog::getDeleted, 0)
                                .orderByDesc(IotAuditLog::getOperationTime)
                                .last("LIMIT 8"))
                .stream()
                .map(item -> {
                    DashboardOverviewVO.RecentOperationVO vo = new DashboardOverviewVO.RecentOperationVO();
                    vo.setOperationId(item.getLogId());
                    vo.setOperator(item.getOperator());
                    vo.setOperationType(assemblerSupport.operationLabel(item.getOperationType()));
                    vo.setDeviceId(item.getDeviceId());
                    vo.setTimestamp(assemblerSupport.format(item.getOperationTime()));
                    return vo;
                })
                .toList();
        overviewVO.setRecentOperations(recentOperations);

        long activated = devices.stream()
                .filter(item -> LifecycleStatus.ACTIVATED.getCode().equals(item.getLifecycleStatus()))
                .count();
        long registered = devices.stream()
                .filter(item -> LifecycleStatus.REGISTERED.getCode().equals(item.getLifecycleStatus()))
                .count();
        DashboardOverviewVO.NameValueVO activatedNode = new DashboardOverviewVO.NameValueVO();
        activatedNode.setName("已激活");
        activatedNode.setValue(activated);
        DashboardOverviewVO.NameValueVO registeredNode = new DashboardOverviewVO.NameValueVO();
        registeredNode.setName("已注册");
        registeredNode.setValue(registered);
        DashboardOverviewVO.NameValueVO frozenNode = new DashboardOverviewVO.NameValueVO();
        frozenNode.setName("已冻结");
        frozenNode.setValue(frozenDevices);
        DashboardOverviewVO.NameValueVO revokedNode = new DashboardOverviewVO.NameValueVO();
        revokedNode.setName("已注销");
        revokedNode.setValue(deactivatedDevices);
        overviewVO.setStatusDistribution(List.of(activatedNode, registeredNode, frozenNode, revokedNode));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<DashboardOverviewVO.TrendVO> trend = IntStream.range(0, 7)
                .mapToObj(i -> {
                    LocalDate date = LocalDate.now().minusDays(6L - i);
                    long value = devices.stream()
                            .filter(item -> item.getRegisterTime() != null
                                    && item.getRegisterTime().toLocalDate().isEqual(date))
                            .count();
                    DashboardOverviewVO.TrendVO trendVO = new DashboardOverviewVO.TrendVO();
                    trendVO.setDate(date.format(dateFormatter));
                    trendVO.setValue(value);
                    return trendVO;
                })
                .toList();
        overviewVO.setAccessTrend(trend);
        return overviewVO;
    }
}
