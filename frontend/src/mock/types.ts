import type { AxiosRequestConfig } from 'axios';

export interface MockApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  total?: number;
}

export function success<T>(data: T, total?: number): MockApiResponse<T> {
  return {
    code: 200,
    message: 'success',
    data,
    total
  };
}

export function fail(message = 'fail', code = 500): MockApiResponse<null> {
  return {
    code,
    message,
    data: null
  };
}

export function parseBody<T>(config: AxiosRequestConfig): T {
  if (!config.data) {
    return {} as T;
  }
  try {
    return JSON.parse(config.data as string) as T;
  } catch {
    return {} as T;
  }
}

export function getParams(config: AxiosRequestConfig): Record<string, any> {
  return (config.params || {}) as Record<string, any>;
}

export function paginate<T>(list: T[], page = 1, pageSize = 10): { list: T[]; total: number } {
  const p = Number(page) || 1;
  const ps = Number(pageSize) || 10;
  const start = (p - 1) * ps;
  return {
    list: list.slice(start, start + ps),
    total: list.length
  };
}
