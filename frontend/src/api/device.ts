import type { AxiosResponse } from 'axios';
import request from '@/utils/request';
import type { ApiResponse, PageParams } from './types';

export interface DeviceRecord {
  deviceId: string;
  deviceName: string;
  did: string;
  deviceType: string;
  vendor: string;
  lifecycleStatus: string;
  registerTime: string;
  description: string;
}

export interface DeviceListQuery extends PageParams {
  deviceId?: string;
  deviceName?: string;
  status?: string;
}

export interface DevicePayload {
  deviceName: string;
  deviceType: string;
  vendor: string;
  description: string;
}

export interface DeviceImportFailure {
  rowNumber: number;
  deviceId?: string;
  deviceName?: string;
  message: string;
}

export interface DeviceImportResult {
  totalRows: number;
  successCount: number;
  failedCount: number;
  createdDeviceIds: string[];
  failures: DeviceImportFailure[];
}

export type DeviceImportTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';

export interface DeviceImportTask {
  taskId: string;
  status: DeviceImportTaskStatus;
  stage: string;
  totalRows: number;
  processedRows: number;
  successCount: number;
  failedCount: number;
  progressPercent: number;
  message?: string;
  startedAt?: number;
  finishedAt?: number;
  result?: DeviceImportResult;
}

export interface DeviceDetail {
  basic: DeviceRecord;
  statusRecords: Array<{
    status: string;
    summaryHash: string;
    blockHeight: number;
    txHash: string;
    time: string;
  }>;
  chainSummary: {
    latestBlockHeight: number;
    latestTxHash: string;
    chainHash: string;
    syncedAt: string;
  };
  recentAudits: Array<{
    logId: string;
    operationType: string;
    operator: string;
    timestamp: string;
    remark: string;
  }>;
}

export function getDeviceList(params: DeviceListQuery): Promise<ApiResponse<DeviceRecord[]>> {
  return request.get('/device/list', { params });
}

export function getDeviceDetail(deviceId: string): Promise<ApiResponse<DeviceDetail>> {
  return request.get(`/device/${deviceId}`);
}

export function createDevice(data: DevicePayload): Promise<ApiResponse<DeviceRecord>> {
  return request.post('/device', data);
}

export function importDevicesByExcel(file: File): Promise<ApiResponse<DeviceImportResult>> {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/device/import', formData);
}

export function startDeviceImportTask(file: File): Promise<ApiResponse<DeviceImportTask>> {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/device/import/task', formData, {
    timeout: 0
  });
}

export function queryDeviceImportTask(taskId: string): Promise<ApiResponse<DeviceImportTask>> {
  return request.get(`/device/import/task/${taskId}`, {
    timeout: 0
  });
}

export function downloadDeviceImportTemplate(): Promise<AxiosResponse<Blob>> {
  return request.get('/device/import/template', {
    responseType: 'blob'
  });
}

export function updateDevice(deviceId: string, data: DevicePayload): Promise<ApiResponse<DeviceRecord>> {
  return request.put(`/device/${deviceId}`, data);
}

export function updateDeviceStatus(deviceId: string, lifecycleStatus: string): Promise<ApiResponse<DeviceRecord>> {
  return request.patch(`/device/${deviceId}/status`, { lifecycleStatus });
}

export function activateDevice(deviceId: string): Promise<ApiResponse<DeviceRecord>> {
  return request.post(`/device/${deviceId}/activate`);
}
