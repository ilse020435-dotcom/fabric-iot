<template>
  <n-tag :type="tagType" round size="small">
    {{ label }}
  </n-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TagProps } from 'naive-ui';

const props = withDefaults(
  defineProps<{
    status: string;
  }>(),
  {
    status: ''
  }
);

const statusMap: Record<string, { type: TagProps['type']; label: string }> = {
  已注册: { type: 'info', label: '已注册' },
  已激活: { type: 'success', label: '已激活' },
  已冻结: { type: 'warning', label: '已冻结' },
  已注销: { type: 'error', label: '已注销' },
  在线: { type: 'success', label: '在线' },
  离线: { type: 'default', label: '离线' },
  异常: { type: 'error', label: '异常' },
  成功: { type: 'success', label: '成功' },
  失败: { type: 'error', label: '失败' },
  已上链: { type: 'success', label: '已上链' },
  未上链: { type: 'warning', label: '未上链' },
  启用: { type: 'success', label: '启用' },
  禁用: { type: 'error', label: '禁用' }
};

const tagType = computed(() => statusMap[props.status]?.type || 'default');
const label = computed(() => statusMap[props.status]?.label || props.status || '未知');
</script>
