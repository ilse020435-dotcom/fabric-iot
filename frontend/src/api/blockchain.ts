import request from '@/utils/request';
import type { ApiResponse, PageParams } from './types';

export interface BlockchainRecord {
  blockHeight: number;
  txHash: string;
  deviceId: string;
  operationType: string;
  timestamp: string;
  chainStatus: string;
  summaryHash: string;
}

export interface BlockchainListQuery extends PageParams {
  deviceId?: string;
  status?: string;
}

export interface BlockchainDetail {
  blockHeight: number;
  txHash: string;
  channelName: string;
  contractName: string;
  writeSet: Record<string, unknown>;
  summaryHash: string;
  payload: Record<string, unknown>;
  timestamp: string;
  chainStatus: string;
}

export function getBlockchainList(params: BlockchainListQuery): Promise<ApiResponse<BlockchainRecord[]>> {
  return request.get('/blockchain/list', { params });
}

export function getBlockchainDetail(txHash: string): Promise<ApiResponse<BlockchainDetail>> {
  return request.get(`/blockchain/${txHash}`);
}
