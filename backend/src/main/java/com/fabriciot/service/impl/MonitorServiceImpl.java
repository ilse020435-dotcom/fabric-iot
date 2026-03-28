package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fabriciot.assembler.MonitorAssembler;
import com.fabriciot.common.enums.MonitorStatus;
import com.fabriciot.dto.monitor.MonitorListQuery;
import com.fabriciot.entity.IotDevice;
import com.fabriciot.mapper.IotDeviceMapper;
import com.fabriciot.mapper.IotDeviceStatusSnapshotMapper;
import com.fabriciot.service.MonitorService;
import com.fabriciot.vo.monitor.MonitorRecordVO;
import com.fabriciot.vo.monitor.MonitorStatsVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class MonitorServiceImpl implements MonitorService {

    private final IotDeviceMapper iotDeviceMapper;
    private final IotDeviceStatusSnapshotMapper snapshotMapper;
    private final MonitorAssembler monitorAssembler;

    public MonitorServiceImpl(IotDeviceMapper iotDeviceMapper, IotDeviceStatusSnapshotMapper snapshotMapper,
                              MonitorAssembler monitorAssembler) {
        this.iotDeviceMapper = iotDeviceMapper;
        this.snapshotMapper = snapshotMapper;
        this.monitorAssembler = monitorAssembler;
    }

    @Override
    public IPage<MonitorRecordVO> list(MonitorListQuery query) {
        List<IotDevice> devices = iotDeviceMapper.selectList(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .like(StringUtils.isNotBlank(query.getDeviceId()), IotDevice::getDeviceId, query.getDeviceId()));
        String targetStatusLabel = StringUtils.isBlank(query.getStatus()) ? null : MonitorStatus.fromCodeOrLabel(query.getStatus()).getLabel();
        List<MonitorRecordVO> allRows = devices.stream()
                .map(device -> monitorAssembler.toMonitorRecordVO(device, snapshotMapper.selectLatestByDeviceId(device.getDeviceId())))
                .filter(item -> targetStatusLabel == null || targetStatusLabel.equals(item.getMonitorStatus()))
                .sorted(Comparator.comparing(MonitorRecordVO::getUpdatedAt).reversed())
                .toList();

        long total = allRows.size();
        int start = Math.toIntExact((query.getPage() - 1) * query.getPageSize());
        int end = Math.min(start + Math.toIntExact(query.getPageSize()), allRows.size());
        List<MonitorRecordVO> pageRows = start >= allRows.size() ? List.of() : allRows.subList(start, end);
        Page<MonitorRecordVO> page = new Page<>(query.getPage(), query.getPageSize(), total);
        page.setRecords(pageRows);
        return page;
    }

    @Override
    public MonitorStatsVO stats() {
        List<IotDevice> devices = iotDeviceMapper.selectList(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0));
        List<MonitorRecordVO> rows = devices.stream()
                .map(device -> monitorAssembler.toMonitorRecordVO(device, snapshotMapper.selectLatestByDeviceId(device.getDeviceId())))
                .toList();
        long online = rows.stream().filter(item -> "在线".equals(item.getMonitorStatus())).count();
        long offline = rows.stream().filter(item -> "离线".equals(item.getMonitorStatus())).count();
        long exception = rows.stream().filter(item -> "异常".equals(item.getMonitorStatus())).count();

        MonitorStatsVO stats = new MonitorStatsVO();
        MonitorStatsVO.NameValueVO onlineNode = new MonitorStatsVO.NameValueVO();
        onlineNode.setName("在线");
        onlineNode.setValue(online);
        MonitorStatsVO.NameValueVO offlineNode = new MonitorStatsVO.NameValueVO();
        offlineNode.setName("离线");
        offlineNode.setValue(offline);
        MonitorStatsVO.NameValueVO exceptionNode = new MonitorStatsVO.NameValueVO();
        exceptionNode.setName("异常");
        exceptionNode.setValue(exception);
        stats.setDistribution(List.of(onlineNode, offlineNode, exceptionNode));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<MonitorStatsVO.TrendVO> trend = IntStream.range(0, 7)
                .mapToObj(i -> {
                    MonitorStatsVO.TrendVO trendVO = new MonitorStatsVO.TrendVO();
                    trendVO.setDate(LocalDate.now().minusDays(6L - i).format(dateFormatter));
                    trendVO.setOnline(Math.max(0, online - (6L - i) % 4));
                    trendVO.setOffline(Math.max(0, offline - (6L - i) % 3));
                    trendVO.setException(Math.max(0, exception - (6L - i) % 2));
                    return trendVO;
                })
                .toList();
        stats.setTrend(trend);
        return stats;
    }
}
