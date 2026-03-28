import request from '@/utils/request';
import type { ApiResponse, PageParams } from './types';

export interface PermissionUser {
  id: number;
  username: string;
  roleId: number | null;
  role: string;
  status: string;
  statusCode: string;
  createdAt: string;
}

export interface PermissionRole {
  id: number;
  roleCode: string;
  roleName: string;
  roleDesc: string;
  permissionCount: number;
}

export interface PermissionTreeNode {
  key: string;
  label: string;
  type: string;
  children?: PermissionTreeNode[];
}

export interface PermissionUserCreatePayload {
  username: string;
  password: string;
  roleId: number;
  status: 'ENABLED' | 'DISABLED';
}

export interface PermissionUserUpdatePayload {
  password?: string;
  roleId: number;
  status: 'ENABLED' | 'DISABLED';
}

export interface PermissionRolePayload {
  roleCode: string;
  roleName: string;
  roleDesc?: string;
}

export interface RolePermissionPayload {
  permissionCodes: string[];
}

export function getPermissionUsers(params: PageParams): Promise<ApiResponse<PermissionUser[]>> {
  return request.get('/permission/users', { params });
}

export function createPermissionUser(data: PermissionUserCreatePayload): Promise<ApiResponse<PermissionUser>> {
  return request.post('/permission/users', data);
}

export function updatePermissionUser(id: number, data: PermissionUserUpdatePayload): Promise<ApiResponse<PermissionUser>> {
  return request.put(`/permission/users/${id}`, data);
}

export function deletePermissionUser(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/permission/users/${id}`);
}

export function getPermissionRoles(params: PageParams): Promise<ApiResponse<PermissionRole[]>> {
  return request.get('/permission/roles', { params });
}

export function createPermissionRole(data: PermissionRolePayload): Promise<ApiResponse<PermissionRole>> {
  return request.post('/permission/roles', data);
}

export function updatePermissionRole(id: number, data: PermissionRolePayload): Promise<ApiResponse<PermissionRole>> {
  return request.put(`/permission/roles/${id}`, data);
}

export function deletePermissionRole(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/permission/roles/${id}`);
}

export function getPermissionTree(): Promise<ApiResponse<PermissionTreeNode[]>> {
  return request.get('/permission/tree');
}

export function getRolePermissionCodes(roleId: number): Promise<ApiResponse<string[]>> {
  return request.get(`/permission/roles/${roleId}/permissions`);
}

export function saveRolePermissionCodes(roleId: number, data: RolePermissionPayload): Promise<ApiResponse<null>> {
  return request.put(`/permission/roles/${roleId}/permissions`, data);
}
