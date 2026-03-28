import MockAdapter from 'axios-mock-adapter';
import request from '@/utils/request';
import { registerAuditMock } from './audit';
import { registerAuthMock } from './auth';
import { registerBlockchainMock } from './blockchain';
import { registerDashboardMock } from './dashboard';
import { registerDeviceMock } from './device';
import { registerMonitorMock } from './monitor';
import { registerPermissionMock } from './permission';

let mocked = false;

export function setupMock() {
  if (mocked) {
    return;
  }

  const mock = new MockAdapter(request, {
    delayResponse: 650,
    onNoMatch: 'passthrough'
  });

  registerAuthMock(mock);
  registerDashboardMock(mock);
  registerDeviceMock(mock);
  registerMonitorMock(mock);
  registerPermissionMock(mock);
  registerAuditMock(mock);
  registerBlockchainMock(mock);

  mocked = true;
}
