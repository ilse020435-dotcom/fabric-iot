<template>
  <page-container title="审计日志" description="查询设备生命周期操作日志及链上留痕明细">
    <search-bar>
      <n-form inline :show-feedback="false">
        <n-form-item label="时间范围">
          <n-date-picker
            v-model:value="timeRange"
            type="datetimerange"
            clearable
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 320px"
          />
        </n-form-item>
        <n-form-item label="操作类型">
          <n-select
            v-model:value="queryForm.operationType"
            clearable
            placeholder="全部操作"
            :options="operationTypeOptions"
            style="width: 180px"
          />
        </n-form-item>
        <n-form-item label="设备ID">
          <n-input v-model:value="queryForm.deviceId" clearable placeholder="请输入设备ID" style="width: 160px" />
        </n-form-item>
        <n-form-item label="操作用户">
          <n-input v-model:value="queryForm.operator" clearable placeholder="请输入操作用户" style="width: 140px" />
        </n-form-item>
      </n-form>
      <template #actions>
        <n-space>
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
      </template>
    </search-bar>

    <n-data-table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="tablePagination"
      :bordered="false"
      :single-line="false"
      remote
    />

    <n-empty v-if="!loading && !tableData.length" description="暂无审计日志" style="margin-top: 16px" />

    <json-viewer-dialog v-model:show="detailVisible" title="审计详情 JSON" :json-data="currentDetail" />
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref, onMounted } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { useMessage } from 'naive-ui';
import { getAuditDetail, getAuditList, type AuditLogRecord } from '@/api/audit';
import JsonViewerDialog from '@/components/JsonViewerDialog.vue';
import PageContainer from '@/components/PageContainer.vue';
import SearchBar from '@/components/SearchBar.vue';
import StatusTag from '@/components/StatusTag.vue';
import TableActions from '@/components/TableActions.vue';

const message = useMessage();

const loading = ref(false);
const detailVisible = ref(false);
const currentDetail = ref<Record<string, unknown>>({});

const tableData = ref<AuditLogRecord[]>([]);

const queryForm = reactive({
  operationType: '',
  deviceId: '',
  operator: ''
});

const timeRange = ref<[string, string] | null>(null);

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
});

const operationTypeOptions = [
  { label: '注册设备', value: '注册设备' },
  { label: '激活设备', value: '激活设备' },
  { label: '冻结设备', value: '冻结设备' },
  { label: '注销设备', value: '注销设备' },
  { label: '更新设备信息', value: '更新设备信息' },
  { label: '同步状态摘要', value: '同步状态摘要' }
];

const tablePagination = computed(() => ({
  page: pagination.page,
  pageSize: pagination.pageSize,
  itemCount: pagination.itemCount,
  showSizePicker: pagination.showSizePicker,
  pageSizes: pagination.pageSizes,
  onUpdatePage: (page: number) => {
    pagination.page = page;
    fetchList();
  },
  onUpdatePageSize: (size: number) => {
    pagination.pageSize = size;
    pagination.page = 1;
    fetchList();
  }
}));

const openDetail = async (row: AuditLogRecord) => {
  try {
    const res = await getAuditDetail(row.logId);
    currentDetail.value = res.data.detail;
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error?.message || '加载详情失败');
  }
};

const columns: DataTableColumns<AuditLogRecord> = [
  { title: '日志ID', key: 'logId', width: 120 },
  { title: '操作时间', key: 'operationTime', width: 170 },
  { title: '操作用户', key: 'operator', width: 110 },
  { title: '操作类型', key: 'operationType', width: 130 },
  { title: '设备ID', key: 'deviceId', width: 120 },
  {
    title: '是否上链',
    key: 'onChain',
    width: 90,
    render: (row) => h(StatusTag, { status: row.onChain })
  },
  { title: '交易哈希', key: 'txHash', width: 220, ellipsis: { tooltip: true } },
  { title: '备注', key: 'remark', width: 220, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'action',
    width: 80,
    fixed: 'right',
    render: (row) =>
      h(TableActions, {
        actions: [{ key: 'detail', label: '查看详情' }],
        onAction: () => openDetail(row)
      })
  }
];

const fetchList = async () => {
  loading.value = true;
  try {
    const [startTime, endTime] = timeRange.value || ['', ''];
    const res = await getAuditList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...queryForm,
      startTime,
      endTime
    });
    tableData.value = res.data;
    pagination.itemCount = res.total || 0;
  } catch (error: any) {
    message.error(error?.message || '审计日志加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  fetchList();
};

const handleReset = () => {
  queryForm.operationType = '';
  queryForm.deviceId = '';
  queryForm.operator = '';
  timeRange.value = null;
  pagination.page = 1;
  fetchList();
};

onMounted(fetchList);
</script>
