import request from '@/utils/request';
import type { ApiResponse, PageParams } from './types';

export interface AuditLogRecord {
  logId: string;
  operationTime: string;
  operator: string;
  operationType: string;
  deviceId: string;
  onChain: string;
  txHash: string;
  remark: string;
  detail: Record<string, unknown>;
}

export interface AuditListQuery extends PageParams {
  startTime?: string;
  endTime?: string;
  operationType?: string;
  deviceId?: string;
  operator?: string;
}

export function getAuditList(params: AuditListQuery): Promise<ApiResponse<AuditLogRecord[]>> {
  return request.get('/audit/list', { params });
}

export function getAuditDetail(logId: string): Promise<ApiResponse<AuditLogRecord>> {
  return request.get(`/audit/${logId}`);
}
