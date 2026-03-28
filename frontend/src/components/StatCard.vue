<template>
  <n-card size="small" :bordered="false" class="stat-card">
    <n-statistic :label="title" :value="value">
      <template v-if="unit" #suffix>
        {{ unit }}
      </template>
    </n-statistic>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </n-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  value: number | string;
  unit?: string;
  hint?: string;
}>();
</script>

<style scoped>
.stat-card {
  background: var(--stat-bg);
  border: 1px solid var(--stat-border);
}

.hint {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
