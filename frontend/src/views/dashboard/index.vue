<template>
  <page-container title="首页仪表盘" description="设备身份生命周期概览与链上运行态势">
    <template #extra>
      <n-button size="small" @click="loadData">刷新数据</n-button>
    </template>

    <n-spin :show="loading">
      <n-grid :x-gap="12" :y-gap="12" :cols="'1 s:2 m:3 l:5'" responsive="screen">
        <n-grid-item>
          <stat-card title="设备总数" :value="overview.stats.totalDevices" hint="已登记 DID 设备" />
        </n-grid-item>
        <n-grid-item>
          <stat-card title="在线设备数" :value="overview.stats.onlineDevices" hint="实时在线监测" />
        </n-grid-item>
        <n-grid-item>
          <stat-card title="冻结设备数" :value="overview.stats.frozenDevices" hint="待人工复核" />
        </n-grid-item>
        <n-grid-item>
          <stat-card title="注销设备数" :value="overview.stats.deactivatedDevices" hint="生命周期终止" />
        </n-grid-item>
        <n-grid-item>
          <stat-card title="今日新增设备" :value="overview.stats.newDevicesToday" hint="当天注册写链" />
        </n-grid-item>
      </n-grid>

      <n-grid :x-gap="12" :y-gap="12" :cols="2" item-responsive responsive="screen" style="margin-top: 12px">
        <n-grid-item span="0:2 800:1">
          <n-card title="设备状态分布图" size="small" :bordered="false">
            <v-chart :option="statusChartOption" autoresize style="height: 300px" />
          </n-card>
        </n-grid-item>
        <n-grid-item span="0:2 800:1">
          <n-card title="最近7天设备接入趋势" size="small" :bordered="false">
            <v-chart :option="trendChartOption" autoresize style="height: 300px" />
          </n-card>
        </n-grid-item>
      </n-grid>

      <n-card title="最近操作记录" size="small" style="margin-top: 12px" :bordered="false">
        <n-data-table
          :columns="columns"
          :data="pagedOperations"
          :pagination="operationTablePagination"
          :bordered="false"
          :single-line="false"
        />
      </n-card>

      <n-empty
        v-if="!loading && !overview.recentOperations.length"
        description="暂无仪表盘数据"
        style="margin-top: 16px"
      />
    </n-spin>
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { useMessage } from 'naive-ui';
import VChart from 'vue-echarts';
import { getDashboardOverview, type DashboardOverview } from '@/api/dashboard';
import PageContainer from '@/components/PageContainer.vue';
import StatCard from '@/components/StatCard.vue';

interface RecentOperation {
  operationId: string;
  operator: string;
  operationType: string;
  deviceId: string;
  timestamp: string;
}

const message = useMessage();
const loading = ref(false);

const overview = reactive<DashboardOverview>({
  stats: {
    totalDevices: 0,
    onlineDevices: 0,
    frozenDevices: 0,
    deactivatedDevices: 0,
    newDevicesToday: 0
  },
  recentOperations: [],
  statusDistribution: [],
  accessTrend: []
});

const columns: DataTableColumns<RecentOperation> = [
  { title: '操作ID', key: 'operationId' },
  { title: '操作用户', key: 'operator' },
  { title: '操作类型', key: 'operationType' },
  { title: '设备ID', key: 'deviceId' },
  { title: '操作时间', key: 'timestamp' }
];

const operationPagination = reactive({
  page: 1,
  pageSize: 5
});

const pagedOperations = computed(() => {
  const start = (operationPagination.page - 1) * operationPagination.pageSize;
  return overview.recentOperations.slice(start, start + operationPagination.pageSize);
});

const operationTablePagination = computed(() => ({
  page: operationPagination.page,
  pageSize: operationPagination.pageSize,
  pageCount: Math.max(Math.ceil(overview.recentOperations.length / operationPagination.pageSize), 1),
  onUpdatePage: (page: number) => {
    operationPagination.page = page;
  }
}));

const statusChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['42%', '70%'],
      label: { formatter: '{b}: {c}' },
      data: overview.statusDistribution
    }
  ],
  color: ['#2f80ed', '#56ccf2', '#f2c94c', '#eb5757']
}));

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: overview.accessTrend.map((item) => item.date)
  },
  yAxis: { type: 'value' },
  series: [
    {
      data: overview.accessTrend.map((item) => item.value),
      type: 'line',
      smooth: true,
      areaStyle: {
        color: 'rgba(47, 128, 237, 0.18)'
      }
    }
  ],
  color: ['#2f80ed']
}));

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getDashboardOverview();
    Object.assign(overview, res.data);
    operationPagination.page = 1;
  } catch (error: any) {
    message.error(error?.message || '加载仪表盘失败');
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>
