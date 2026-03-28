import { h } from 'vue';
import { NIcon, type MenuOption } from 'naive-ui';
import {
  CubeOutline,
  DocumentTextOutline,
  GridOutline,
  HardwareChipOutline,
  PulseOutline,
  SettingsOutline
} from '@vicons/ionicons5';

const renderIcon = (icon: any) => () => h(NIcon, null, { default: () => h(icon) });

export const sidebarMenuOptions: MenuOption[] = [
  {
    label: '首页仪表盘',
    key: '/dashboard',
    icon: renderIcon(GridOutline)
  },
  {
    label: '设备管理',
    key: '/device',
    icon: renderIcon(HardwareChipOutline)
  },
  {
    label: '状态监控',
    key: '/monitor',
    icon: renderIcon(PulseOutline)
  },
  {
    label: '管理平台',
    key: 'management',
    icon: renderIcon(SettingsOutline),
    children: [
      {
        label: '权限管理',
        key: '/permission'
      },
      {
        label: 'Fabric诊断',
        key: '/fabric-diagnostic'
      }
    ]
  },
  {
    label: '审计日志',
    key: '/audit',
    icon: renderIcon(DocumentTextOutline)
  },
  {
    label: '区块链记录',
    key: '/blockchain',
    icon: renderIcon(CubeOutline)
  }
];
