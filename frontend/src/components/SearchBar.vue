<template>
  <n-card size="small" :bordered="false" class="search-bar">
    <div class="search-grid">
      <div class="fields">
        <slot />
      </div>
      <div class="actions">
        <slot name="actions" />
      </div>
    </div>
  </n-card>
</template>

<style scoped>
.search-bar {
  margin-bottom: 12px;
  background: var(--search-bg);
  border: 1px solid var(--panel-border);
}

.search-grid {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.fields {
  flex: 1;
}

.actions {
  flex-shrink: 0;
}

@media (max-width: 992px) {
  .search-grid {
    flex-direction: column;
    align-items: stretch;
  }

  .actions {
    text-align: right;
  }
}
</style>
