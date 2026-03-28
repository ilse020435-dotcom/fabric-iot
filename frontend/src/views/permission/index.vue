<template>
  <page-container title="权限管理" description="用户、角色与菜单/按钮权限配置">
    <template #extra>
      <n-space>
        <n-button
          v-if="canUserManage"
          :type="activeTab === 'user' ? 'primary' : 'default'"
          @click="activeTab = 'user'"
        >
          用户管理
        </n-button>
        <n-button
          v-if="canRoleManage"
          :type="activeTab === 'role' ? 'primary' : 'default'"
          @click="activeTab = 'role'"
        >
          角色管理
        </n-button>
      </n-space>
    </template>

    <permission-user v-if="activeTab === 'user' && canUserManage" />
    <permission-role v-if="activeTab === 'role' && canRoleManage" />
    <n-empty
      v-if="!canUserManage && !canRoleManage"
      description="当前账号无权限管理访问权限"
      style="margin-top: 24px"
    />
  </page-container>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue';
import PageContainer from '@/components/PageContainer.vue';
import PermissionUser from './user.vue';
import PermissionRole from './role.vue';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();

const canUserManage = computed(() => userStore.permissions.includes('user:manage'));
const canRoleManage = computed(() => userStore.permissions.includes('role:manage'));

const activeTab = ref<'user' | 'role'>('user');

watchEffect(() => {
  if (activeTab.value === 'user' && !canUserManage.value && canRoleManage.value) {
    activeTab.value = 'role';
  }
  if (activeTab.value === 'role' && !canRoleManage.value && canUserManage.value) {
    activeTab.value = 'user';
  }
});
</script>
