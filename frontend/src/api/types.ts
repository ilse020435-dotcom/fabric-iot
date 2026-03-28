export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  total?: number;
}

export interface PageParams {
  page: number;
  pageSize: number;
}
