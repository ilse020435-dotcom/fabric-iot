import { createRouter, createWebHistory } from 'vue-router';
import BasicLayout from '@/layout/BasicLayout.vue';
import { clearToken, clearUser, getUser, TOKEN_KEY } from '@/utils/storage';
import { canAccessMenu } from '@/utils/role';

const firstAccessiblePath = (permissions: string[]) =>
  ['/dashboard', '/device', '/monitor', '/permission', '/management/fabric-diagnostic', '/audit', '/blockchain'].find((path) =>
    canAccessMenu(permissions, path)
  );

// @ts-ignore
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: {
        title: '登录',
        requiresAuth: false
      }
    },
    {
      path: '/',
      component: BasicLayout,
      redirect: '/dashboard',
      meta: {
        requiresAuth: true
      },
      children: [
        {
          path: '/dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '首页仪表盘' }
        },
        {
          path: '/device',
          name: 'Device',
          component: () => import('@/views/device/index.vue'),
          meta: { title: '设备管理' }
        },
        {
          path: '/device/:id',
          name: 'DeviceDetail',
          component: () => import('@/views/device/detail.vue'),
          meta: { title: '设备详情' }
        },
        {
          path: '/monitor',
          name: 'Monitor',
          component: () => import('@/views/monitor/index.vue'),
          meta: { title: '状态监控' }
        },
        {
          path: '/permission',
          name: 'Permission',
          component: () => import('@/views/permission/index.vue'),
          meta: { title: '权限管理' }
        },
        {
          path: '/fabric-diagnostic',
          name: 'FabricDiagnostic',
          component: () => import('@/views/fabric/index.vue'),
          meta: { title: 'Fabric诊断' }
        },
        {
          path: '/audit',
          name: 'Audit',
          component: () => import('@/views/audit/index.vue'),
          meta: { title: '审计日志' }
        },
        {
          path: '/blockchain',
          name: 'Blockchain',
          component: () => import('@/views/blockchain/index.vue'),
          meta: { title: '区块链记录' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
});

router.beforeEach((to, _from, next) => {
  const clearAuth = () => {
    clearToken();
    clearUser();
  };

  const token = localStorage.getItem(TOKEN_KEY);
  const user = getUser();
  const permissions = user?.permissions || [];

  if (to.path === '/login') {
    if (token && user && permissions.length) {
      next(firstAccessiblePath(permissions) || '/dashboard');
      return;
    }
    if (token && (!user || !permissions.length)) {
      clearAuth();
    }
    next();
    return;
  }

  if (to.meta.requiresAuth === false) {
    next();
    return;
  }

  if (!token || !user || !permissions.length) {
    clearAuth();
    next('/login');
    return;
  }

  const menuKey = to.path.startsWith('/device/') ? '/device' : to.path;
  if (!canAccessMenu(permissions, menuKey)) {
    const fallbackPath = firstAccessiblePath(permissions);
    if (fallbackPath) {
      next(fallbackPath);
      return;
    }
    clearAuth();
    next('/login');
    return;
  }

  next();
});

export default router;
