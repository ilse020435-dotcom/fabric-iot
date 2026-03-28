import type MockAdapter from 'axios-mock-adapter';
import { auditLogs, devices, getRecent7DaysTrend, monitorRecords, sortByTimeDesc } from './data';
import { success } from './types';

export function registerDashboardMock(mock: MockAdapter) {
  mock.onGet('/dashboard/overview').reply(() => {
    const totalDevices = devices.length;
    const onlineDevices = monitorRecords.filter((item) => item.monitorStatus === '在线').length;
    const frozenDevices = devices.filter((item) => item.lifecycleStatus === '已冻结').length;
    const deactivatedDevices = devices.filter((item) => item.lifecycleStatus === '已注销').length;
    const today = new Date().toISOString().slice(0, 10);
    const newDevicesToday = devices.filter((item) => item.registerTime.startsWith(today)).length;

    const recentOperations = sortByTimeDesc(auditLogs, 'operationTime').slice(0, 8).map((item) => ({
      operationId: item.logId,
      operator: item.operator,
      operationType: item.operationType,
      deviceId: item.deviceId,
      timestamp: item.operationTime
    }));

    const statusDistribution = [
      { name: '已激活', value: devices.filter((item) => item.lifecycleStatus === '已激活').length },
      { name: '已注册', value: devices.filter((item) => item.lifecycleStatus === '已注册').length },
      { name: '已冻结', value: frozenDevices },
      { name: '已注销', value: deactivatedDevices }
    ];

    return [
      200,
      success({
        stats: {
          totalDevices,
          onlineDevices,
          frozenDevices,
          deactivatedDevices,
          newDevicesToday
        },
        recentOperations,
        statusDistribution,
        accessTrend: getRecent7DaysTrend()
      })
    ];
  });
}
