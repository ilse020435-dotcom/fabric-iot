<template>
  <page-container title="状态监控" description="监控设备在线状态、状态摘要 hash 与链上交易映射">
    <search-bar>
      <n-form inline :show-feedback="false" :model="queryForm">
        <n-form-item label="设备ID">
          <n-input v-model:value="queryForm.deviceId" clearable placeholder="请输入设备ID" style="width: 180px" />
        </n-form-item>
        <n-form-item label="状态">
          <n-select
            v-model:value="queryForm.status"
            clearable
            placeholder="全部状态"
            :options="statusOptions"
            style="width: 150px"
          />
        </n-form-item>
      </n-form>
      <template #actions>
        <n-space>
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-space>
      </template>
    </search-bar>

    <n-grid :x-gap="12" :y-gap="12" :cols="2" item-responsive responsive="screen" style="margin-bottom: 12px">
      <n-grid-item span="0:2 900:1">
        <n-card title="状态分布统计" size="small" :bordered="false">
          <v-chart autoresize :option="distributionOption" style="height: 280px" />
        </n-card>
      </n-grid-item>
      <n-grid-item span="0:2 900:1">
        <n-card title="状态变化趋势" size="small" :bordered="false">
          <v-chart autoresize :option="trendOption" style="height: 280px" />
        </n-card>
      </n-grid-item>
    </n-grid>

    <n-data-table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="tablePagination"
      :bordered="false"
      remote
      :single-line="false"
    />

    <n-empty v-if="!loading && !tableData.length" description="暂无监控数据" style="margin-top: 16px" />

    <n-modal v-model:show="detailVisible" preset="card" title="监控详情" style="width: 680px">
      <n-descriptions v-if="currentRow" label-placement="left" bordered :column="1">
        <n-descriptions-item label="设备ID">{{ currentRow.deviceId }}</n-descriptions-item>
        <n-descriptions-item label="设备名称">{{ currentRow.deviceName }}</n-descriptions-item>
        <n-descriptions-item label="当前状态">
          <status-tag :status="currentRow.monitorStatus" />
        </n-descriptions-item>
        <n-descriptions-item label="状态摘要 Hash">{{ currentRow.summaryHash }}</n-descriptions-item>
        <n-descriptions-item label="区块高度">{{ currentRow.blockHeight }}</n-descriptions-item>
        <n-descriptions-item label="交易哈希">{{ currentRow.txHash }}</n-descriptions-item>
        <n-descriptions-item label="信号强度">{{ currentRow.signalStrength }}</n-descriptions-item>
        <n-descriptions-item label="设备温度">{{ currentRow.temperature }}</n-descriptions-item>
        <n-descriptions-item label="最近更新时间">{{ currentRow.updatedAt }}</n-descriptions-item>
      </n-descriptions>
    </n-modal>
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { useMessage } from 'naive-ui';
import VChart from 'vue-echarts';
import { getMonitorList, getMonitorStats, type MonitorRecord, type MonitorStats } from '@/api/monitor';
import PageContainer from '@/components/PageContainer.vue';
import SearchBar from '@/components/SearchBar.vue';
import StatusTag from '@/components/StatusTag.vue';
import TableActions from '@/components/TableActions.vue';

const message = useMessage();
const loading = ref(false);
const tableData = ref<MonitorRecord[]>([]);
const detailVisible = ref(false);
const currentRow = ref<MonitorRecord | null>(null);

const queryForm = reactive({
  deviceId: '',
  status: ''
});

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
});

const statusOptions = [
  { label: '在线', value: '在线' },
  { label: '离线', value: '离线' },
  { label: '异常', value: '异常' }
];

const monitorStats = reactive<MonitorStats>({
  distribution: [],
  trend: []
});

const distributionOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['45%', '72%'],
      data: monitorStats.distribution,
      label: { formatter: '{b}: {c}' }
    }
  ],
  color: ['#2f80ed', '#9aa5b8', '#eb5757']
}));

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  xAxis: {
    type: 'category',
    data: monitorStats.trend.map((item) => item.date)
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '在线',
      type: 'line',
      smooth: true,
      data: monitorStats.trend.map((item) => item.online)
    },
    {
      name: '离线',
      type: 'line',
      smooth: true,
      data: monitorStats.trend.map((item) => item.offline)
    },
    {
      name: '异常',
      type: 'line',
      smooth: true,
      data: monitorStats.trend.map((item) => item.exception)
    }
  ],
  color: ['#2f80ed', '#9aa5b8', '#eb5757']
}));

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

const openDetail = (row: MonitorRecord) => {
  currentRow.value = row;
  detailVisible.value = true;
};

const columns: DataTableColumns<MonitorRecord> = [
  { title: '设备ID', key: 'deviceId', width: 120 },
  { title: '设备名称', key: 'deviceName', width: 140 },
  {
    title: '当前状态',
    key: 'monitorStatus',
    width: 100,
    render: (row) => h(StatusTag, { status: row.monitorStatus })
  },
  { title: '最近更新时间', key: 'updatedAt', width: 170 },
  { title: '状态摘要 Hash', key: 'summaryHash', width: 220, ellipsis: { tooltip: true } },
  { title: '区块高度', key: 'blockHeight', width: 100 },
  { title: '交易哈希', key: 'txHash', width: 220, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'action',
    width: 90,
    fixed: 'right',
    render: (row) =>
      h(TableActions, {
        actions: [{ key: 'view', label: '查看详情' }],
        onAction: () => openDetail(row)
      })
  }
];

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await getMonitorList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...queryForm
    });
    tableData.value = res.data;
    pagination.itemCount = res.total || 0;
  } catch (error: any) {
    message.error(error?.message || '监控列表加载失败');
  } finally {
    loading.value = false;
  }
};

const fetchStats = async () => {
  try {
    const res = await getMonitorStats();
    Object.assign(monitorStats, res.data);
  } catch (error: any) {
    message.error(error?.message || '监控图表加载失败');
  }
};

const handleSearch = () => {
  pagination.page = 1;
  fetchList();
};

const handleReset = () => {
  queryForm.deviceId = '';
  queryForm.status = '';
  pagination.page = 1;
  fetchList();
};

onMounted(() => {
  fetchList();
  fetchStats();
});
</script>
