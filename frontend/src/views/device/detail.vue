<template>
  <page-container title="设备详情" description="查看设备 DID、生命周期状态记录、链上摘要与审计轨迹">
    <template #extra>
      <n-space>
        <n-button @click="goBack">返回列表</n-button>
        <n-button type="primary" @click="fetchDetail">刷新详情</n-button>
      </n-space>
    </template>

    <n-spin :show="loading">
      <n-empty v-if="!loading && !detail" description="未找到设备详情" />

      <template v-else-if="detail">
        <n-grid :x-gap="12" :y-gap="12" :cols="2" item-responsive responsive="screen">
          <n-grid-item span="0:2 900:1">
            <n-card title="基础信息" size="small" :bordered="false">
              <n-descriptions label-placement="left" bordered :column="1" size="small">
                <n-descriptions-item label="设备ID">{{ detail.basic.deviceId }}</n-descriptions-item>
                <n-descriptions-item label="设备名称">{{ detail.basic.deviceName }}</n-descriptions-item>
                <n-descriptions-item label="设备类型">{{ detail.basic.deviceType }}</n-descriptions-item>
                <n-descriptions-item label="厂商">{{ detail.basic.vendor }}</n-descriptions-item>
                <n-descriptions-item label="生命周期状态">
                  <status-tag :status="detail.basic.lifecycleStatus" />
                </n-descriptions-item>
                <n-descriptions-item label="注册时间">{{ detail.basic.registerTime }}</n-descriptions-item>
              </n-descriptions>
            </n-card>
          </n-grid-item>

          <n-grid-item span="0:2 900:1">
            <n-card title="DID 与链上摘要" size="small" :bordered="false">
              <n-descriptions label-placement="left" bordered :column="1" size="small">
                <n-descriptions-item label="DID 标识">{{ detail.basic.did }}</n-descriptions-item>
                <n-descriptions-item label="最新区块高度">{{ detail.chainSummary.latestBlockHeight }}</n-descriptions-item>
                <n-descriptions-item label="最新交易哈希">{{ detail.chainSummary.latestTxHash }}</n-descriptions-item>
                <n-descriptions-item label="状态摘要 Hash">{{ detail.chainSummary.chainHash }}</n-descriptions-item>
                <n-descriptions-item label="最近同步时间">{{ detail.chainSummary.syncedAt }}</n-descriptions-item>
              </n-descriptions>
            </n-card>
          </n-grid-item>
        </n-grid>

        <n-card title="最近状态记录" size="small" :bordered="false" style="margin-top: 12px">
          <n-data-table
            :columns="statusColumns"
            :data="pagedStatusRecords"
            :pagination="statusPagination"
            :bordered="false"
          />
        </n-card>

        <n-card title="最近审计记录" size="small" :bordered="false" style="margin-top: 12px">
          <n-data-table
            :columns="auditColumns"
            :data="pagedRecentAudits"
            :pagination="auditPagination"
            :bordered="false"
          />
        </n-card>
      </template>
    </n-spin>
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMessage, type DataTableColumns } from 'naive-ui';
import { getDeviceDetail, type DeviceDetail } from '@/api/device';
import PageContainer from '@/components/PageContainer.vue';
import StatusTag from '@/components/StatusTag.vue';

const route = useRoute();
const router = useRouter();
const message = useMessage();

const loading = ref(false);
const detail = ref<DeviceDetail | null>(null);
const statusPage = ref(1);
const auditPage = ref(1);

const pageSize = 5;

const pagedStatusRecords = computed(() => {
  const all = detail.value?.statusRecords || [];
  const start = (statusPage.value - 1) * pageSize;
  return all.slice(start, start + pageSize);
});

const pagedRecentAudits = computed(() => {
  const all = detail.value?.recentAudits || [];
  const start = (auditPage.value - 1) * pageSize;
  return all.slice(start, start + pageSize);
});

const statusPagination = computed(() => ({
  page: statusPage.value,
  pageSize,
  pageCount: Math.max(Math.ceil((detail.value?.statusRecords.length || 0) / pageSize), 1),
  onUpdatePage: (page: number) => {
    statusPage.value = page;
  }
}));

const auditPagination = computed(() => ({
  page: auditPage.value,
  pageSize,
  pageCount: Math.max(Math.ceil((detail.value?.recentAudits.length || 0) / pageSize), 1),
  onUpdatePage: (page: number) => {
    auditPage.value = page;
  }
}));

const statusColumns: DataTableColumns<any> = [
  {
    title: '状态',
    key: 'status',
    render: (row) => h(StatusTag, { status: row.status })
  },
  { title: '状态摘要 Hash', key: 'summaryHash', ellipsis: { tooltip: true } },
  { title: '区块高度', key: 'blockHeight' },
  { title: '交易哈希', key: 'txHash', ellipsis: { tooltip: true } },
  { title: '时间', key: 'time' }
];

const auditColumns: DataTableColumns<any> = [
  { title: '日志ID', key: 'logId' },
  { title: '操作类型', key: 'operationType' },
  { title: '操作用户', key: 'operator' },
  { title: '操作时间', key: 'timestamp' },
  { title: '备注', key: 'remark' }
];

const fetchDetail = async () => {
  const id = String(route.params.id || '');
  if (!id) return;

  loading.value = true;
  try {
    const res = await getDeviceDetail(id);
    detail.value = res.data;
    statusPage.value = 1;
    auditPage.value = 1;
  } catch (error: any) {
    message.error(error?.message || '设备详情加载失败');
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.push('/device');
};

onMounted(fetchDetail);
</script>
