import type MockAdapter from 'axios-mock-adapter';
import { getRecent7DaysTrend, monitorRecords } from './data';
import { getParams, paginate, success } from './types';

export function registerMonitorMock(mock: MockAdapter) {
  mock.onGet('/monitor/list').reply((config) => {
    const { page = 1, pageSize = 10, deviceId = '', status = '' } = getParams(config);

    const filtered = monitorRecords
      .filter((item) => (deviceId ? item.deviceId.includes(String(deviceId)) : true))
      .filter((item) => (status ? item.monitorStatus === status : true))
      .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));

    const { list, total } = paginate(filtered, Number(page), Number(pageSize));
    return [200, success(list, total)];
  });

  mock.onGet('/monitor/stats').reply(() => {
    const distribution = [
      { name: '在线', value: monitorRecords.filter((item) => item.monitorStatus === '在线').length },
      { name: '离线', value: monitorRecords.filter((item) => item.monitorStatus === '离线').length },
      { name: '异常', value: monitorRecords.filter((item) => item.monitorStatus === '异常').length }
    ];

    const baseTrend = getRecent7DaysTrend();
    const trend = baseTrend.map((item, idx) => ({
      date: item.date,
      online: 16 + (idx * 3) % 8,
      offline: 6 + (idx * 2) % 5,
      exception: 2 + (idx + 1) % 3
    }));

    return [200, success({ distribution, trend })];
  });
}
