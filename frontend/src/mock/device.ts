import type MockAdapter from 'axios-mock-adapter';
import { auditLogs, devices, generateHash, generateTime, monitorRecords } from './data';
import { fail, getParams, paginate, parseBody, success } from './types';

interface DevicePayload {
  deviceName: string;
  deviceType: string;
  vendor: string;
  description: string;
}

export function registerDeviceMock(mock: MockAdapter) {
  mock.onGet('/device/list').reply((config) => {
    const { page = 1, pageSize = 10, deviceId = '', deviceName = '', status = '' } = getParams(config);

    const filtered = devices
      .filter((item) => (deviceId ? item.deviceId.includes(String(deviceId)) : true))
      .filter((item) => (deviceName ? item.deviceName.includes(String(deviceName)) : true))
      .filter((item) => (status ? item.lifecycleStatus === status : true))
      .sort((a, b) => b.registerTime.localeCompare(a.registerTime));

    const { list, total } = paginate(filtered, Number(page), Number(pageSize));
    return [200, success(list, total)];
  });

  mock.onGet(/\/device\/DEV-\d+$/).reply((config) => {
    const deviceId = config.url?.split('/').pop() || '';
    const target = devices.find((item) => item.deviceId === deviceId);

    if (!target) {
      return [200, fail('设备不存在', 404)];
    }

    const statusRecords = Array.from({ length: 6 }, (_, idx) => ({
      status: ['已注册', '已激活', '已冻结', '已激活', '已冻结', target.lifecycleStatus][idx],
      summaryHash: generateHash(idx + deviceId.length, 40),
      blockHeight: 62000 + idx,
      txHash: generateHash(idx + 500, 64),
      time: generateTime((idx + 2) * 130)
    }));

    const recentAudits = auditLogs
      .filter((log) => log.deviceId === deviceId)
      .slice(0, 6)
      .map((log) => ({
        logId: log.logId,
        operationType: log.operationType,
        operator: log.operator,
        timestamp: log.operationTime,
        remark: log.remark
      }));

    return [
      200,
      success({
        basic: target,
        statusRecords,
        chainSummary: {
          latestBlockHeight: 73654,
          latestTxHash: generateHash(deviceId.length + 88, 64),
          chainHash: generateHash(deviceId.length + 77, 40),
          syncedAt: generateTime(8)
        },
        recentAudits
      })
    ];
  });

  mock.onPost('/device').reply((config) => {
    const payload = parseBody<DevicePayload>(config);
    if (!payload.deviceName || !payload.deviceType || !payload.vendor) {
      return [200, fail('请补全设备信息', 400)];
    }

    const idNum = 1000 + devices.length + 1;
    const deviceId = `DEV-${idNum}`;
    const record = {
      deviceId,
      deviceName: payload.deviceName,
      did: `did:fabric:iot:${deviceId.toLowerCase()}`,
      deviceType: payload.deviceType,
      vendor: payload.vendor,
      lifecycleStatus: '已注册',
      registerTime: generateTime(0),
      description: payload.description || ''
    };

    devices.unshift(record);
    monitorRecords.unshift({
      deviceId,
      deviceName: payload.deviceName,
      monitorStatus: '离线',
      updatedAt: generateTime(0),
      summaryHash: generateHash(idNum, 48),
      blockHeight: 70000 + idNum,
      txHash: generateHash(idNum + 66, 64),
      signalStrength: '80%',
      temperature: '25℃'
    });

    auditLogs.unshift({
      logId: `AUD-${6000 + auditLogs.length}`,
      operationTime: generateTime(0),
      operator: 'admin',
      operationType: '注册设备',
      deviceId,
      onChain: '已上链',
      txHash: generateHash(idNum + 300, 64),
      remark: '新增设备并写入链上身份',
      detail: {
        payload,
        did: record.did,
        summaryHash: generateHash(idNum + 320, 40)
      }
    });

    return [200, success(record)];
  });

  mock.onPut(/\/device\/DEV-\d+$/).reply((config) => {
    const deviceId = config.url?.split('/').pop() || '';
    const payload = parseBody<DevicePayload>(config);

    const target = devices.find((item) => item.deviceId === deviceId);
    if (!target) {
      return [200, fail('设备不存在', 404)];
    }

    target.deviceName = payload.deviceName;
    target.deviceType = payload.deviceType;
    target.vendor = payload.vendor;
    target.description = payload.description;

    const monitor = monitorRecords.find((item) => item.deviceId === deviceId);
    if (monitor) {
      monitor.deviceName = payload.deviceName;
      monitor.updatedAt = generateTime(0);
    }

    auditLogs.unshift({
      logId: `AUD-${6000 + auditLogs.length}`,
      operationTime: generateTime(0),
      operator: 'admin',
      operationType: '更新设备信息',
      deviceId,
      onChain: '已上链',
      txHash: generateHash(deviceId.length + 360, 64),
      remark: '设备元数据变更并同步链上摘要',
      detail: {
        payload,
        summaryHash: generateHash(deviceId.length + 390, 40)
      }
    });

    return [200, success(target)];
  });

  mock.onPatch(/\/device\/DEV-\d+\/status$/).reply((config) => {
    const matched = config.url?.match(/\/device\/(DEV-\d+)\/status$/);
    const deviceId = matched?.[1] || '';
    const { lifecycleStatus } = parseBody<{ lifecycleStatus: string }>(config);
    const target = devices.find((item) => item.deviceId === deviceId);

    if (!target) {
      return [200, fail('设备不存在', 404)];
    }

    target.lifecycleStatus = lifecycleStatus;

    const monitor = monitorRecords.find((item) => item.deviceId === deviceId);
    if (monitor) {
      monitor.monitorStatus = lifecycleStatus === '已冻结' ? '异常' : lifecycleStatus === '已注销' ? '离线' : '在线';
      monitor.updatedAt = generateTime(0);
      monitor.summaryHash = generateHash(target.deviceId.length + Date.now() % 1000, 48);
    }

    auditLogs.unshift({
      logId: `AUD-${6000 + auditLogs.length}`,
      operationTime: generateTime(0),
      operator: 'admin',
      operationType: lifecycleStatus === '已冻结' ? '冻结设备' : lifecycleStatus === '已注销' ? '注销设备' : '激活设备',
      deviceId,
      onChain: '已上链',
      txHash: generateHash(target.deviceId.length + 430, 64),
      remark: `设备状态更新为${lifecycleStatus}`,
      detail: {
        lifecycleStatus,
        summaryHash: generateHash(target.deviceId.length + 451, 40)
      }
    });

    return [200, success(target)];
  });
}
