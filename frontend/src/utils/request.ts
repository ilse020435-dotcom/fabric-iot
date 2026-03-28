import axios, { type AxiosError, type AxiosResponse } from 'axios';
import { createDiscreteApi } from 'naive-ui';
import router from '@/router';
import { clearToken, clearUser, getToken } from './storage';

const { dialog } = createDiscreteApi(['dialog']);
let authExpiredDialogVisible = false;

const isAuthEndpoint = (url?: string) => {
  if (!url) {
    return false;
  }
  return url.includes('/auth/login') || url.includes('/auth/logout');
};

const redirectToLoginWithDialog = (message?: string) => {
  if (authExpiredDialogVisible || router.currentRoute.value.path === '/login') {
    return;
  }
  authExpiredDialogVisible = true;
  clearToken();
  clearUser();

  dialog.warning({
    title: '登录已失效',
    content: message || '登录状态已过期，请重新登录。',
    positiveText: '去登录',
    closable: false,
    maskClosable: false,
    onPositiveClick: () => {
      authExpiredDialogVisible = false;
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login');
      }
    }
  });
};

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
});

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = {
      ...(config.headers || {}),
      Authorization: `Bearer ${token}`
    } as any;
  }
  return config;
});

request.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config?.responseType === 'blob') {
      return response;
    }
    const result = response.data;
    if (result?.code === 401 && !isAuthEndpoint(response.config?.url)) {
      redirectToLoginWithDialog(result?.message);
    }
    if (result?.code !== 200) {
      return Promise.reject(new Error(result?.message || '请求失败'));
    }
    return result;
  },
  (error: AxiosError<any>) => {
    const responseCode = error.response?.data?.code;
    const statusCode = error.response?.status;
    if ((responseCode === 401 || statusCode === 401) && !isAuthEndpoint(error.config?.url)) {
      redirectToLoginWithDialog(error.response?.data?.message || '登录状态已失效');
    }
    return Promise.reject(error);
  }
);

export default request;
