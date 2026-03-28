package com.fabriciot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.dto.device.DeviceListQuery;
import com.fabriciot.dto.device.DeviceSaveRequest;
import com.fabriciot.dto.device.DeviceStatusReportRequest;
import com.fabriciot.vo.device.DeviceDetailVO;
import com.fabriciot.vo.device.DeviceListVO;
import com.fabriciot.vo.device.LifecycleEventVO;

import java.util.List;

public interface DeviceService {

    IPage<DeviceListVO> list(DeviceListQuery query);

    DeviceDetailVO detail(String deviceId);

    DeviceListVO create(DeviceSaveRequest request, String operator);

    DeviceListVO update(String deviceId, DeviceSaveRequest request, String operator);

    DeviceListVO changeStatus(String deviceId, String lifecycleStatus, String operator);

    DeviceListVO freeze(String deviceId, String operator);

    DeviceListVO revoke(String deviceId, String operator);

    DeviceListVO activate(String deviceId, String operator);

    List<LifecycleEventVO> lifecycleRecords(String deviceId, Integer limit);

    void reportStatus(String deviceId, DeviceStatusReportRequest request, String operator);
}

