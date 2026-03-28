<template>
  <page-container title="区块链记录" description="模拟区块链浏览器，查看设备生命周期操作上链交易">
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

    <n-data-table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="tablePagination"
      :bordered="false"
      :single-line="false"
      remote
    />

    <n-empty v-if="!loading && !tableData.length" description="暂无链上记录" style="margin-top: 16px" />

    <n-modal v-model:show="detailVisible" preset="card" title="链上交易详情" style="width: 760px">
      <template v-if="detail">
        <n-card size="small" title="区块信息" :bordered="false">
          <n-descriptions bordered :column="1" size="small">
            <n-descriptions-item label="区块高度">{{ detail.blockHeight }}</n-descriptions-item>
            <n-descriptions-item label="交易哈希">{{ detail.txHash }}</n-descriptions-item>
            <n-descriptions-item label="通道名称">{{ detail.channelName }}</n-descriptions-item>
            <n-descriptions-item label="合约名称">{{ detail.contractName }}</n-descriptions-item>
            <n-descriptions-item label="时间戳">{{ detail.timestamp }}</n-descriptions-item>
            <n-descriptions-item label="状态">
              <status-tag :status="detail.chainStatus" />
            </n-descriptions-item>
          </n-descriptions>
        </n-card>

        <n-card size="small" title="交易内容" :bordered="false" style="margin-top: 12px">
          <pre class="json-viewer">{{ JSON.stringify(detail.payload || {}, null, 2) }}</pre>
        </n-card>

        <n-card size="small" title="上链摘要" :bordered="false" style="margin-top: 12px">
          <n-descriptions bordered :column="1" size="small">
            <n-descriptions-item label="摘要Hash">{{ detail.summaryHash }}</n-descriptions-item>
            <n-descriptions-item label="写集键">{{ detail.writeSet?.key || '--' }}</n-descriptions-item>
            <n-descriptions-item label="写集版本">{{ detail.writeSet?.version || '--' }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </template>
    </n-modal>
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { useMessage } from 'naive-ui';
import { getBlockchainDetail, getBlockchainList, type BlockchainDetail, type BlockchainRecord } from '@/api/blockchain';
import PageContainer from '@/components/PageContainer.vue';
import SearchBar from '@/components/SearchBar.vue';
import StatusTag from '@/components/StatusTag.vue';
import TableActions from '@/components/TableActions.vue';

const message = useMessage();
const loading = ref(false);
const tableData = ref<BlockchainRecord[]>([]);
const detailVisible = ref(false);
const detail = ref<BlockchainDetail | null>(null);

const queryForm = reactive({
  deviceId: '',
  status: ''
});

const statusOptions = [
  { label: '成功', value: '成功' },
  { label: '失败', value: '失败' }
];

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
});

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

const openDetail = async (row: BlockchainRecord) => {
  try {
    const res = await getBlockchainDetail(row.txHash);
    detail.value = res.data;
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error?.message || '链上详情加载失败');
  }
};

const columns: DataTableColumns<BlockchainRecord> = [
  { title: '区块高度', key: 'blockHeight', width: 100 },
  { title: '交易哈希', key: 'txHash', width: 230, ellipsis: { tooltip: true } },
  { title: '设备ID', key: 'deviceId', width: 120 },
  { title: '操作类型', key: 'operationType', width: 130 },
  { title: '时间戳', key: 'timestamp', width: 170 },
  {
    title: '状态',
    key: 'chainStatus',
    width: 90,
    render: (row) => h(StatusTag, { status: row.chainStatus })
  },
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
    const res = await getBlockchainList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...queryForm
    });
    tableData.value = res.data;
    pagination.itemCount = res.total || 0;
  } catch (error: any) {
    message.error(error?.message || '区块链记录加载失败');
  } finally {
    loading.value = false;
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

onMounted(fetchList);
</script>
