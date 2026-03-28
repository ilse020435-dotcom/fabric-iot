<template>
  <n-modal
    :show="show"
    preset="card"
    style="width: 760px"
    :title="title"
    @update:show="$emit('update:show', $event)"
  >
    <pre class="json-viewer">{{ formattedJson }}</pre>
  </n-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    show: boolean;
    title?: string;
    jsonData?: unknown;
  }>(),
  {
    title: 'JSON 详情',
    jsonData: null
  }
);

defineEmits<{
  'update:show': [value: boolean];
}>();

const formattedJson = computed(() => JSON.stringify(props.jsonData ?? {}, null, 2));
</script>
