const HEX = '0123456789abcdef';

function makeHash(seed: number, len = 64): string {
  let text = '';
  for (let i = 0; i < len; i += 1) {
    text += HEX[(seed * 17 + i * 13 + 11) % 16];
  }
  return `0x${text}`;
}

function formatDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
}

const DEVICE_TYPES = ['工业传感器', '边缘网关', '智能摄像头', '环境采集终端'];
const VENDORS = ['华星智联', '澜舟科技', '云启设备', '北斗感知'];
const LIFECYCLE_STATUS = ['已注册', '已激活', '已冻结', '已注销'];
const MONITOR_STATUS = ['在线', '离线', '异常'];
const OPERATION_TYPES = ['注册设备', '激活设备', '冻结设备', '注销设备', '更新设备信息', '同步状态摘要'];
const OPERATORS = ['admin', 'operator_a', 'auditor_b', 'regulator_c'];

export interface DeviceEntity {
  deviceId: string;
  deviceName: string;
  did: string;
  deviceType: string;
  vendor: string;
  lifecycleStatus: string;
  registerTime: string;
  description: string;
}

export interface MonitorEntity {
  deviceId: string;
  deviceName: string;
  monitorStatus: string;
  updatedAt: string;
  summaryHash: string;
  blockHeight: number;
  txHash: string;
  signalStrength: string;
  temperature: string;
}

export interface AuditEntity {
  logId: string;
  operationTime: string;
  operator: string;
  operationType: string;
  deviceId: string;
  onChain: string;
  txHash: string;
  remark: string;
  detail: Record<string, unknown>;
}

export interface BlockchainEntity {
  blockHeight: number;
  txHash: string;
  deviceId: string;
  operationType: string;
  timestamp: string;
  chainStatus: string;
  summaryHash: string;
}

const now = Date.now();

export const devices: DeviceEntity[] = Array.from({ length: 56 }, (_, idx) => {
  const index = idx + 1;
  const deviceId = `DEV-${String(1000 + index)}`;
  const lifecycleStatus = LIFECYCLE_STATUS[idx % LIFECYCLE_STATUS.length];
  const registerDate = new Date(now - (idx % 24) * 24 * 3600 * 1000 - (idx % 7) * 3600 * 1000);
  return {
    deviceId,
    deviceName: `物联设备-${String(index).padStart(3, '0')}`,
    did: `did:fabric:iot:${deviceId.toLowerCase()}`,
    deviceType: DEVICE_TYPES[idx % DEVICE_TYPES.length],
    vendor: VENDORS[idx % VENDORS.length],
    lifecycleStatus,
    registerTime: formatDate(registerDate),
    description: `用于生产线身份认证与状态上链的${DEVICE_TYPES[idx % DEVICE_TYPES.length]}`
  };
});

export const monitorRecords: MonitorEntity[] = devices.map((item, idx) => {
  const monitorStatus =
    item.lifecycleStatus === '已冻结'
      ? '异常'
      : item.lifecycleStatus === '已注销'
        ? '离线'
        : MONITOR_STATUS[idx % MONITOR_STATUS.length];
  const updatedAt = formatDate(new Date(now - (idx % 12) * 2 * 3600 * 1000));
  return {
    deviceId: item.deviceId,
    deviceName: item.deviceName,
    monitorStatus,
    updatedAt,
    summaryHash: makeHash(idx + 9, 48),
    blockHeight: 41000 + idx * 3,
    txHash: makeHash(idx + 30, 64),
    signalStrength: `${76 - (idx % 18)}%`,
    temperature: `${22 + (idx % 9)}℃`
  };
});

export const auditLogs: AuditEntity[] = Array.from({ length: 90 }, (_, idx) => {
  const device = devices[idx % devices.length];
  const operationType = OPERATION_TYPES[idx % OPERATION_TYPES.length];
  const onChain = idx % 6 === 0 ? '未上链' : '已上链';
  const operationTime = formatDate(new Date(now - idx * 2 * 3600 * 1000));
  const txHash = onChain === '已上链' ? makeHash(idx + 120, 64) : '--';

  return {
    logId: `AUD-${String(5000 + idx)}`,
    operationTime,
    operator: OPERATORS[idx % OPERATORS.length],
    operationType,
    deviceId: device.deviceId,
    onChain,
    txHash,
    remark: `${operationType}操作完成，状态摘要已校验`,
    detail: {
      beforeStatus: LIFECYCLE_STATUS[(idx + 1) % LIFECYCLE_STATUS.length],
      afterStatus: LIFECYCLE_STATUS[idx % LIFECYCLE_STATUS.length],
      did: device.did,
      summaryHash: makeHash(idx + 200, 40),
      extension: {
        operatorDept: '设备运营中心',
        ipAddress: `10.20.1.${(idx % 40) + 10}`
      }
    }
  };
});

export const blockchainRecords: BlockchainEntity[] = auditLogs
  .filter((item) => item.onChain === '已上链')
  .map((item, idx) => ({
    blockHeight: 50000 + idx,
    txHash: item.txHash,
    deviceId: item.deviceId,
    operationType: item.operationType,
    timestamp: item.operationTime,
    chainStatus: idx % 10 === 0 ? '失败' : '成功',
    summaryHash: makeHash(idx + 250, 40)
  }));

export const permissionUsers = Array.from({ length: 18 }, (_, idx) => {
  const roleId = (idx % 4) + 1;
  const statusCode = idx % 5 === 0 ? 'DISABLED' : 'ENABLED';
  return {
    id: idx + 1,
    username: `user_${idx + 1}`,
    roleId,
    role: ['系统管理员', '设备运营方', '设备厂商', '监管机构'][idx % 4],
    status: statusCode === 'DISABLED' ? '禁用' : '启用',
    statusCode,
    createdAt: formatDate(new Date(now - idx * 86400000))
  };
});

export const permissionRoles = [
  { id: 1, roleCode: 'ADMIN', roleName: '系统管理员', roleDesc: '拥有全量配置与审计权限', permissionCount: 13 },
  { id: 2, roleCode: 'OPERATOR', roleName: '设备运营方', roleDesc: '负责设备激活、冻结和监控', permissionCount: 9 },
  { id: 3, roleCode: 'VENDOR', roleName: '设备厂商', roleDesc: '负责设备初始注册与元数据维护', permissionCount: 7 },
  { id: 4, roleCode: 'REGULATOR', roleName: '监管机构', roleDesc: '拥有审计和链上记录查询权限', permissionCount: 6 }
];

export const permissionTree = [
  { key: 'dashboard:view', label: '首页查看', type: 'MENU' },
  {
    key: 'device',
    label: '设备管理',
    type: 'MENU',
    children: [
      { key: 'device:list', label: '设备列表查看', type: 'MENU' },
      { key: 'device:create', label: '设备注册', type: 'BUTTON' },
      { key: 'device:update', label: '设备编辑', type: 'BUTTON' },
      { key: 'device:freeze', label: '设备冻结', type: 'BUTTON' },
      { key: 'device:revoke', label: '设备注销', type: 'BUTTON' }
    ]
  },
  { key: 'monitor:view', label: '状态监控', type: 'MENU' },
  { key: 'audit:view', label: '审计日志查看', type: 'MENU' },
  { key: 'blockchain:view', label: '区块链记录查看', type: 'MENU' },
  {
    key: 'permission',
    label: '权限管理',
    type: 'MENU',
    children: [
      { key: 'user:manage', label: '用户管理', type: 'BUTTON' },
      { key: 'role:manage', label: '角色管理', type: 'BUTTON' }
    ]
  }
];

export const permissionRolePermissions: Record<number, string[]> = {
  1: [
    'dashboard:view',
    'device',
    'device:list',
    'device:create',
    'device:update',
    'device:freeze',
    'device:revoke',
    'monitor:view',
    'audit:view',
    'blockchain:view',
    'permission',
    'user:manage',
    'role:manage'
  ],
  2: [
    'dashboard:view',
    'device',
    'device:list',
    'device:update',
    'device:freeze',
    'device:revoke',
    'monitor:view',
    'audit:view',
    'blockchain:view'
  ],
  3: [
    'dashboard:view',
    'device',
    'device:list',
    'device:create',
    'device:update',
    'monitor:view',
    'blockchain:view'
  ],
  4: ['dashboard:view', 'device', 'device:list', 'monitor:view', 'audit:view', 'blockchain:view']
};

export function generateHash(seed: number, len = 64): string {
  return makeHash(seed, len);
}

export function generateTime(offsetMinutes = 0): string {
  return formatDate(new Date(Date.now() - offsetMinutes * 60000));
}

export function getRecent7DaysTrend() {
  return Array.from({ length: 7 }, (_, idx) => {
    const date = new Date(now - (6 - idx) * 86400000);
    return {
      date: formatDate(new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0)).slice(0, 10),
      value: 12 + ((idx + 3) * 7) % 20
    };
  });
}

export function sortByTimeDesc<T>(list: T[], key: keyof T): T[] {
  return [...list].sort((a, b) => String(b[key]).localeCompare(String(a[key])));
}
