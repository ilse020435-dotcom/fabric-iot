import type MockAdapter from 'axios-mock-adapter';
import { permissionRolePermissions, permissionRoles, permissionTree, permissionUsers } from './data';
import { fail, getParams, paginate, parseBody, success } from './types';

interface UserCreatePayload {
  username: string;
  password: string;
  roleId: number;
  status: 'ENABLED' | 'DISABLED';
}

interface UserUpdatePayload {
  password?: string;
  roleId: number;
  status: 'ENABLED' | 'DISABLED';
}

interface RolePayload {
  roleCode: string;
  roleName: string;
  roleDesc?: string;
}

interface RolePermissionPayload {
  permissionCodes: string[];
}

function statusLabel(statusCode: string): string {
  return statusCode === 'DISABLED' ? '禁用' : '启用';
}

function updateRolePermissionCount(roleId: number) {
  const role = permissionRoles.find((item) => item.id === roleId);
  if (!role) {
    return;
  }
  role.permissionCount = (permissionRolePermissions[roleId] || []).length;
}

function updateAllRolePermissionCount() {
  permissionRoles.forEach((item) => updateRolePermissionCount(item.id));
}

function extractId(url?: string): number {
  if (!url) {
    return 0;
  }
  const match = url.match(/\/(\d+)(?:\/permissions)?$/);
  return match ? Number(match[1]) : 0;
}

export function registerPermissionMock(mock: MockAdapter) {
  updateAllRolePermissionCount();

  mock.onGet('/permission/users').reply((config) => {
    const { page = 1, pageSize = 10 } = getParams(config);
    const { list, total } = paginate(permissionUsers, Number(page), Number(pageSize));
    return [200, success(list, total)];
  });

  mock.onPost('/permission/users').reply((config) => {
    const body = parseBody<UserCreatePayload>(config);
    if (!body.username || !body.password || !body.roleId) {
      return [200, fail('参数不完整', 400)];
    }
    if (permissionUsers.some((item) => item.username === body.username)) {
      return [200, fail('用户名已存在', 422)];
    }
    const role = permissionRoles.find((item) => item.id === Number(body.roleId));
    if (!role) {
      return [200, fail('角色不存在', 404)];
    }
    const nextId = permissionUsers.length ? Math.max(...permissionUsers.map((item) => item.id)) + 1 : 1;
    const user = {
      id: nextId,
      username: body.username,
      roleId: role.id,
      role: role.roleName,
      statusCode: body.status === 'DISABLED' ? 'DISABLED' : 'ENABLED',
      status: statusLabel(body.status),
      createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19)
    };
    permissionUsers.unshift(user);
    return [200, success(user)];
  });

  mock.onPut(/\/permission\/users\/\d+$/).reply((config) => {
    const userId = extractId(config.url);
    const user = permissionUsers.find((item) => item.id === userId);
    if (!user) {
      return [200, fail('用户不存在', 404)];
    }
    const body = parseBody<UserUpdatePayload>(config);
    const role = permissionRoles.find((item) => item.id === Number(body.roleId));
    if (!role) {
      return [200, fail('角色不存在', 404)];
    }
    user.roleId = role.id;
    user.role = role.roleName;
    user.statusCode = body.status === 'DISABLED' ? 'DISABLED' : 'ENABLED';
    user.status = statusLabel(user.statusCode);
    return [200, success(user)];
  });

  mock.onDelete(/\/permission\/users\/\d+$/).reply((config) => {
    const userId = extractId(config.url);
    const index = permissionUsers.findIndex((item) => item.id === userId);
    if (index < 0) {
      return [200, fail('用户不存在', 404)];
    }
    permissionUsers.splice(index, 1);
    return [200, success(null)];
  });

  mock.onGet('/permission/roles').reply((config) => {
    const { page = 1, pageSize = 10 } = getParams(config);
    updateAllRolePermissionCount();
    const { list, total } = paginate(permissionRoles, Number(page), Number(pageSize));
    return [200, success(list, total)];
  });

  mock.onPost('/permission/roles').reply((config) => {
    const body = parseBody<RolePayload>(config);
    if (!body.roleCode || !body.roleName) {
      return [200, fail('参数不完整', 400)];
    }
    const roleCode = body.roleCode.toUpperCase();
    if (permissionRoles.some((item) => item.roleCode === roleCode)) {
      return [200, fail('角色编码已存在', 422)];
    }
    const nextId = permissionRoles.length ? Math.max(...permissionRoles.map((item) => item.id)) + 1 : 1;
    const role = {
      id: nextId,
      roleCode,
      roleName: body.roleName,
      roleDesc: body.roleDesc || '',
      permissionCount: 0
    };
    permissionRoles.unshift(role);
    permissionRolePermissions[nextId] = [];
    return [200, success(role)];
  });

  mock.onPut(/\/permission\/roles\/\d+$/).reply((config) => {
    const roleId = extractId(config.url);
    const role = permissionRoles.find((item) => item.id === roleId);
    if (!role) {
      return [200, fail('角色不存在', 404)];
    }
    const body = parseBody<RolePayload>(config);
    if (!body.roleCode || !body.roleName) {
      return [200, fail('参数不完整', 400)];
    }
    const roleCode = body.roleCode.toUpperCase();
    const duplicated = permissionRoles.find((item) => item.roleCode === roleCode && item.id !== roleId);
    if (duplicated) {
      return [200, fail('角色编码已存在', 422)];
    }
    role.roleCode = roleCode;
    role.roleName = body.roleName;
    role.roleDesc = body.roleDesc || '';
    updateRolePermissionCount(roleId);
    return [200, success(role)];
  });

  mock.onDelete(/\/permission\/roles\/\d+$/).reply((config) => {
    const roleId = extractId(config.url);
    if (permissionUsers.some((item) => item.roleId === roleId)) {
      return [200, fail('该角色已绑定用户，无法删除', 422)];
    }
    const index = permissionRoles.findIndex((item) => item.id === roleId);
    if (index < 0) {
      return [200, fail('角色不存在', 404)];
    }
    permissionRoles.splice(index, 1);
    delete permissionRolePermissions[roleId];
    return [200, success(null)];
  });

  mock.onGet('/permission/tree').reply(200, success(permissionTree));

  mock.onGet(/\/permission\/roles\/\d+\/permissions$/).reply((config) => {
    const roleId = extractId(config.url);
    return [200, success(permissionRolePermissions[roleId] || [])];
  });

  mock.onPut(/\/permission\/roles\/\d+\/permissions$/).reply((config) => {
    const roleId = extractId(config.url);
    const role = permissionRoles.find((item) => item.id === roleId);
    if (!role) {
      return [200, fail('角色不存在', 404)];
    }
    const body = parseBody<RolePermissionPayload>(config);
    permissionRolePermissions[roleId] = Array.from(new Set(body.permissionCodes || []));
    updateRolePermissionCount(roleId);
    return [200, success(null)];
  });
}
