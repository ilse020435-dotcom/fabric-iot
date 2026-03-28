import request from '@/utils/request';
import type { ApiResponse, PageParams } from './types';

export interface DashboardOverview {
  stats: {
    totalDevices: number;
    onlineDevices: number;
    frozenDevices: number;
    deactivatedDevices: number;
    newDevicesToday: number;
  };
  recentOperations: Array<{
    operationId: string;
    operator: string;
    operationType: string;
    deviceId: string;
    timestamp: string;
  }>;
  statusDistribution: Array<{
    name: string;
    value: number;
  }>;
  accessTrend: Array<{
    date: string;
    value: number;
  }>;
}

export function getDashboardOverview(): Promise<ApiResponse<DashboardOverview>> {
  return request.get('/dashboard/overview');
}
