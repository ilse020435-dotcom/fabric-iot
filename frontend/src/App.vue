<template>
  <n-config-provider
    :theme="isDarkMode ? darkTheme : undefined"
    :theme-overrides="themeOverrides"
    :locale="zhCN"
    :date-locale="dateZhCN"
  >
    <n-loading-bar-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-message-provider>
            <router-view />
          </n-message-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import { dateZhCN, darkTheme, zhCN } from 'naive-ui';
import { useAppStore } from '@/stores/app';

const appStore = useAppStore();

const isDarkMode = computed(() => appStore.darkMode);

watch(
  isDarkMode,
  (value) => {
    document.body.classList.toggle('dark-mode', value);
  },
  { immediate: true }
);

const themeOverrides = {
  common: {
    primaryColor: '#1f6feb',
    primaryColorHover: '#3a7cf5',
    primaryColorPressed: '#185bca'
  }
};
</script>
