import request from '@/utils/request';
import type { ApiResponse, PageParams } from './types';

export interface MonitorRecord {
  deviceId: string;
  deviceName: string;
  monitorStatus: string;
  updatedAt: string;
  summaryHash: string;
  blockHeight: number;
  txHash: string;
  signalStrength: string;
  temperature: string;
}

export interface MonitorListQuery extends PageParams {
  deviceId?: string;
  status?: string;
}

export interface MonitorStats {
  distribution: Array<{ name: string; value: number }>;
  trend: Array<{ date: string; online: number; offline: number; exception: number }>;
}

export function getMonitorList(params: MonitorListQuery): Promise<ApiResponse<MonitorRecord[]>> {
  return request.get('/monitor/list', { params });
}

export function getMonitorStats(): Promise<ApiResponse<MonitorStats>> {
  return request.get('/monitor/stats');
}
