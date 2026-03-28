import request from '@/utils/request';
import type { ApiResponse } from './types';

export interface LoginParams {
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  username: string;
  role: string;
  permissions: string[];
}

export function loginApi(data: LoginParams): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/login', data);
}

export function logoutApi(): Promise<ApiResponse<null>> {
  return request.post('/auth/logout');
}
