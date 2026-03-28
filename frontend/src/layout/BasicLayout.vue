<template>
  <n-layout has-sider style="height: 100%">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="230"
      :collapsed="appStore.siderCollapsed"
      show-trigger="arrow-circle"
      @collapse="appStore.siderCollapsed = true"
      @expand="appStore.siderCollapsed = false"
    >
      <div class="logo-area">IoT-Chain</div>
      <n-menu
        :collapsed="appStore.siderCollapsed"
        :collapsed-width="64"
        :collapsed-icon-size="20"
        :options="visibleMenuOptions"
        :value="activeMenu"
        @update:value="handleMenuSelect"
      />
    </n-layout-sider>

    <n-layout>
      <n-layout-header bordered class="header">
        <div class="left-area">
          <n-button quaternary circle @click="appStore.toggleSider()">
            <template #icon>
              <n-icon>
                <menu-outline />
              </n-icon>
            </template>
          </n-button>
          <div class="title-wrap">
            <h1 class="system-title">{{ systemTitle }}</h1>
            <n-breadcrumb>
              <n-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                {{ item.title }}
              </n-breadcrumb-item>
            </n-breadcrumb>
          </div>
        </div>

        <n-space align="center" :size="14">
          <span class="user-text">{{ userStore.username || '未登录' }}（{{ userStore.role || '未知角色' }}）</span>
          <n-switch :value="appStore.darkMode" @update:value="appStore.setDarkMode">
            <template #checked>
              暗
            </template>
            <template #unchecked>
              亮
            </template>
          </n-switch>
          <n-button type="error" ghost size="small" @click="handleLogout">退出登录</n-button>
        </n-space>
      </n-layout-header>

      <n-layout-content content-style="padding: 16px; min-height: calc(100vh - 72px);">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { MenuOption } from 'naive-ui';
import { MenuOutline } from '@vicons/ionicons5';
import { sidebarMenuOptions } from '@/constants/menu';
import { SYSTEM_TITLE } from '@/constants/system';
import { useAppStore } from '@/stores/app';
import { useUserStore } from '@/stores/user';
import { canAccessMenu } from '@/utils/role';

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const userStore = useUserStore();
const systemTitle = SYSTEM_TITLE;

function filterMenuByPermission(options: MenuOption[]): MenuOption[] {
  return options.reduce<MenuOption[]>((result, option) => {
    const rawChildren = (option as any).children as MenuOption[] | undefined;
    if (rawChildren?.length) {
      const children = filterMenuByPermission(rawChildren);
      if (children.length) {
        result.push({
          ...(option as any),
          children
        } as MenuOption);
      }
      return result;
    }

    if (canAccessMenu(userStore.permissions, String(option.key))) {
      result.push(option);
    }
    return result;
  }, []);
}

const visibleMenuOptions = computed(() =>
  filterMenuByPermission(sidebarMenuOptions)
);

const activeMenu = computed(() => {
  if (route.path.startsWith('/device/')) {
    return '/device';
  }
  return route.path;
});

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title)
    .map((item) => ({
      path: item.path,
      title: item.meta.title as string
    }))
);

const handleMenuSelect = (key: string) => {
  if (!key.startsWith('/')) {
    return;
  }
  router.push(key);
};

const handleLogout = async () => {
  await userStore.logout();
  router.push('/login');
};
</script>

<style scoped>
.logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: var(--logo-color);
  border-bottom: 1px solid var(--logo-border);
  letter-spacing: 0.5px;
}

.header {
  height: 72px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--header-bg);
}

.left-area {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.system-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--header-title);
}

.user-text {
  color: var(--header-user);
  font-size: 13px;
}

@media (max-width: 1080px) {
  .system-title {
    font-size: 14px;
  }

  .user-text {
    display: none;
  }
}
</style>
