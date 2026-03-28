import request from '@/utils/request';
import type { ApiResponse } from './types';

export interface FabricDiagnosticParams {
  function?: string;
  args?: string[];
}

export interface FabricDiagnosticResult {
  enabled: boolean;
  mock: boolean;
  tlsEnabled: boolean;
  certConfigured: boolean;
  keyConfigured: boolean;
  tlsCertConfigured: boolean;
  gatewayConfigReady: boolean;
  tcpReachable: boolean;
  gatewayReady: boolean;
  chaincodeInvocationRequested: boolean;
  chaincodeInvocationSuccess: boolean;
  basicConnectionSuccess: boolean;
  endToEndSuccess: boolean;
  success: boolean;
  peerEndpoint: string;
  mspId: string;
  channelName: string;
  chaincodeName: string;
  contractName: string;
  function: string | null;
  args: string[];
  evaluateResult: string | null;
  message: string;
  error: string | null;
  issues: string[];
}

export function runFabricDiagnostic(params: FabricDiagnosticParams = {}): Promise<ApiResponse<FabricDiagnosticResult>> {
  const searchParams = new URLSearchParams();
  const functionName = params.function?.trim();
  if (functionName) {
    searchParams.set('function', functionName);
  }
  (params.args || [])
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
    .forEach((item) => searchParams.append('args', item));

  const query = searchParams.toString();
  return request.get(`/fabric/test${query ? `?${query}` : ''}`);
}
