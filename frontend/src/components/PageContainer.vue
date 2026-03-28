<template>
  <div class="page-container">
    <div class="header">
      <div>
        <h2 class="page-section-title">{{ title }}</h2>
        <p v-if="description" class="page-section-desc">{{ description }}</p>
      </div>
      <div>
        <slot name="extra" />
      </div>
    </div>
    <div class="content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.content {
  min-height: 120px;
}
</style>
