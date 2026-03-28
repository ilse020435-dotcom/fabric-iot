<template>
  <n-space :size="6">
    <n-button
      v-for="action in actions"
      :key="action.key"
      text
      size="small"
      :type="action.type || 'primary'"
      :loading="action.loading"
      :disabled="action.disabled"
      @click="$emit('action', action.key)"
    >
      {{ action.label }}
    </n-button>
  </n-space>
</template>

<script setup lang="ts">
interface ActionItem {
  key: string;
  label: string;
  type?: 'default' | 'primary' | 'success' | 'warning' | 'error';
  loading?: boolean;
  disabled?: boolean;
}

defineProps<{
  actions: ActionItem[];
}>();

defineEmits<{
  action: [key: string];
}>();
</script>
