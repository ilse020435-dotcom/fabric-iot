import type MockAdapter from 'axios-mock-adapter';
import { blockchainRecords } from './data';
import { fail, getParams, paginate, success } from './types';

export function registerBlockchainMock(mock: MockAdapter) {
  mock.onGet('/blockchain/list').reply((config) => {
    const { page = 1, pageSize = 10, deviceId = '', status = '' } = getParams(config);

    const filtered = blockchainRecords
      .filter((item) => (deviceId ? item.deviceId.includes(String(deviceId)) : true))
      .filter((item) => (status ? item.chainStatus === status : true))
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp));

    const { list, total } = paginate(filtered, Number(page), Number(pageSize));
    return [200, success(list, total)];
  });

  mock.onGet(/\/blockchain\/0x[0-9a-f]+$/).reply((config) => {
    const txHash = config.url?.split('/').pop() || '';
    const target = blockchainRecords.find((item) => item.txHash === txHash);

    if (!target) {
      return [200, fail('交易不存在', 404)];
    }

    return [
      200,
      success({
        ...target,
        channelName: 'iotchannel',
        contractName: 'deviceLifecycleCC',
        writeSet: {
          key: `${target.deviceId}:state`,
          version: `${target.blockHeight}:1`
        },
        payload: {
          deviceId: target.deviceId,
          operationType: target.operationType,
          summaryHash: target.summaryHash,
          endorsedBy: ['org1.peer0', 'org2.peer0']
        }
      })
    ];
  });
}
