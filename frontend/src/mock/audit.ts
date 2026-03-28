import type MockAdapter from 'axios-mock-adapter';
import { auditLogs } from './data';
import { fail, getParams, paginate, success } from './types';

export function registerAuditMock(mock: MockAdapter) {
  mock.onGet('/audit/list').reply((config) => {
    const {
      page = 1,
      pageSize = 10,
      startTime = '',
      endTime = '',
      operationType = '',
      deviceId = '',
      operator = ''
    } = getParams(config);

    const filtered = auditLogs
      .filter((item) => (operationType ? item.operationType === operationType : true))
      .filter((item) => (deviceId ? item.deviceId.includes(String(deviceId)) : true))
      .filter((item) => (operator ? item.operator.includes(String(operator)) : true))
      .filter((item) => {
        if (!startTime || !endTime) return true;
        return item.operationTime >= startTime && item.operationTime <= endTime;
      })
      .sort((a, b) => b.operationTime.localeCompare(a.operationTime));

    const { list, total } = paginate(filtered, Number(page), Number(pageSize));
    return [200, success(list, total)];
  });

  mock.onGet(/\/audit\/AUD-\d+$/).reply((config) => {
    const logId = config.url?.split('/').pop() || '';
    const target = auditLogs.find((item) => item.logId === logId);
    if (!target) {
      return [200, fail('日志不存在', 404)];
    }
    return [200, success(target)];
  });
}
