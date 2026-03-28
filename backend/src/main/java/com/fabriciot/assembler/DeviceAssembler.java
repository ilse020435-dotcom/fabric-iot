package com.fabriciot.assembler;

import com.fabriciot.entity.IotDevice;
import com.fabriciot.entity.IotDeviceLifecycleEvent;
import com.fabriciot.entity.IotDeviceStatusSnapshot;
import com.fabriciot.vo.device.DeviceDetailVO;
import com.fabriciot.vo.device.DeviceListVO;
import com.fabriciot.vo.device.LifecycleEventVO;
import org.springframework.stereotype.Component;

@Component
public class DeviceAssembler {

    private final AssemblerSupport support;

    public DeviceAssembler(AssemblerSupport support) {
        this.support = support;
    }

    public DeviceListVO toListVO(IotDevice entity) {
        DeviceListVO vo = new DeviceListVO();
        vo.setDeviceId(entity.getDeviceId());
        vo.setDeviceName(entity.getDeviceName());
        vo.setDid(entity.getDid());
        vo.setDeviceType(entity.getDeviceType());
        vo.setVendor(entity.getVendor());
        vo.setLifecycleStatus(support.lifecycleLabel(entity.getLifecycleStatus()));
        vo.setRegisterTime(support.format(entity.getRegisterTime()));
        vo.setDescription(entity.getDescription());
        return vo;
    }

    public LifecycleEventVO toLifecycleVO(IotDeviceLifecycleEvent entity) {
        LifecycleEventVO vo = new LifecycleEventVO();
        vo.setOperationType(support.operationLabel(entity.getOperationType()));
        vo.setBeforeStatus(entity.getBeforeStatus() == null ? "" : support.lifecycleLabel(entity.getBeforeStatus()));
        vo.setAfterStatus(support.lifecycleLabel(entity.getAfterStatus()));
        vo.setSummaryHash(entity.getSummaryHash());
        vo.setOperator(entity.getOperator());
        vo.setOccurredAt(support.format(entity.getOccurredAt()));
        vo.setTxHash(entity.getTxHash() == null ? "--" : entity.getTxHash());
        return vo;
    }

    public DeviceDetailVO.StatusRecordVO toStatusRecordVO(IotDeviceStatusSnapshot snapshot) {
        DeviceDetailVO.StatusRecordVO vo = new DeviceDetailVO.StatusRecordVO();
        vo.setStatus(support.monitorLabel(snapshot.getMonitorStatus()));
        vo.setSummaryHash(snapshot.getSummaryHash());
        vo.setBlockHeight(snapshot.getBlockHeight() == null ? 0L : snapshot.getBlockHeight());
        vo.setTxHash(snapshot.getTxHash() == null ? "--" : snapshot.getTxHash());
        vo.setTime(support.format(snapshot.getUpdatedAt()));
        return vo;
    }
}

