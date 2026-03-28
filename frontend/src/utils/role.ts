const menuPermissionMap: Record<string, string | string[]> = {
  '/dashboard': 'dashboard:view',
  '/device': 'device:list',
  '/monitor': 'monitor:view',
  '/permission': ['user:manage', 'role:manage'],
  '/fabric-diagnostic': 'fabric:diagnose',
  '/audit': 'audit:view',
  '/blockchain': 'blockchain:view'
};

function hasPermission(permissions: string[], permission: string): boolean {
  return permissions.includes(permission);
}

export function canAccessMenu(permissions: string[], key: string): boolean {
  const requiredPermission = menuPermissionMap[key];
  if (!requiredPermission) {
    return true;
  }
  if (Array.isArray(requiredPermission)) {
    return requiredPermission.some((item) => hasPermission(permissions, item));
  }
  return hasPermission(permissions, requiredPermission);
}

export function canCreateDevice(permissions: string[]): boolean {
  return hasPermission(permissions, 'device:create');
}

export function canImportDevice(permissions: string[]): boolean {
  return hasPermission(permissions, 'device:import');
}

export function canEditDevice(permissions: string[]): boolean {
  return hasPermission(permissions, 'device:update');
}

export function canActivateDevice(permissions: string[]): boolean {
  return hasPermission(permissions, 'device:update');
}

export function canFreezeDevice(permissions: string[]): boolean {
  return hasPermission(permissions, 'device:freeze');
}

export function canRevokeDevice(permissions: string[]): boolean {
  return hasPermission(permissions, 'device:revoke');
}
