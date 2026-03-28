import type MockAdapter from 'axios-mock-adapter';
import { permissionRolePermissions, permissionUsers } from './data';
import { fail, parseBody, success } from './types';

interface LoginPayload {
  username: string;
  password: string;
}

export function registerAuthMock(mock: MockAdapter) {
  mock.onPost('/auth/login').reply((config) => {
    const { username, password } = parseBody<LoginPayload>(config);

    if (!username || !password) {
      return [200, fail('用户名或密码不能为空', 400)];
    }

    const foundUser = permissionUsers.find((item) => item.username === username);
    const role = foundUser?.role || '设备运营方';
    const permissions = foundUser?.roleId
      ? permissionRolePermissions[foundUser.roleId] || []
      : permissionRolePermissions[2] || [];

    return [
      200,
      success({
        token: `mock-token-${Date.now()}`,
        username,
        role,
        permissions
      })
    ];
  });

  mock.onPost('/auth/logout').reply(200, success(null));
}
