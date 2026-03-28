package com.fabriciot.assembler;

import com.fabriciot.entity.IotDevice;
import com.fabriciot.entity.IotDeviceStatusSnapshot;
import com.fabriciot.vo.monitor.MonitorRecordVO;
import org.springframework.stereotype.Component;

@Component
public class MonitorAssembler {

    private final AssemblerSupport support;

    public MonitorAssembler(AssemblerSupport support) {
        this.support = support;
    }

    public MonitorRecordVO toMonitorRecordVO(IotDevice device, IotDeviceStatusSnapshot snapshot) {
        MonitorRecordVO vo = new MonitorRecordVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        if (snapshot == null) {
            vo.setMonitorStatus("离线");
            vo.setUpdatedAt(support.format(device.getUpdatedAt()));
            vo.setSummaryHash("--");
            vo.setBlockHeight(0L);
            vo.setTxHash("--");
            vo.setSignalStrength("--");
            vo.setTemperature("--");
            return vo;
        }
        vo.setMonitorStatus(support.monitorLabel(snapshot.getMonitorStatus()));
        vo.setUpdatedAt(support.format(snapshot.getUpdatedAt()));
        vo.setSummaryHash(snapshot.getSummaryHash());
        vo.setBlockHeight(snapshot.getBlockHeight() == null ? 0L : snapshot.getBlockHeight());
        vo.setTxHash(snapshot.getTxHash() == null ? "--" : snapshot.getTxHash());
        vo.setSignalStrength(snapshot.getSignalStrength() == null ? "--" : snapshot.getSignalStrength());
        vo.setTemperature(snapshot.getTemperature() == null ? "--" : snapshot.getTemperature());
        return vo;
    }
}
