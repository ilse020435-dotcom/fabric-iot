<template>
  <page-container title="设备管理" description="管理设备 DID 身份、生命周期状态及基础信息">
    <search-bar>
      <n-form inline :show-feedback="false" :model="queryForm">
        <n-form-item label="设备ID">
          <n-input v-model:value="queryForm.deviceId" placeholder="请输入设备ID" clearable style="width: 180px" />
        </n-form-item>
        <n-form-item label="设备名称">
          <n-input v-model:value="queryForm.deviceName" placeholder="请输入设备名称" clearable style="width: 180px" />
        </n-form-item>
        <n-form-item label="状态">
          <n-select
            v-model:value="queryForm.status"
            clearable
            placeholder="全部状态"
            :options="statusOptions"
            style="width: 160px"
          />
        </n-form-item>
      </n-form>
      <template #actions>
        <n-space>
          <n-button type="primary" @click="handleSearch">查询</n-button>
          <n-button @click="handleReset">重置</n-button>
          <n-button v-if="canImport" @click="openImportModal">Excel导入</n-button>
          <n-button v-if="canCreate" type="info" @click="openCreateModal">新增设备</n-button>
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

    <n-empty v-if="!loading && !tableData.length" description="暂无设备数据" style="margin-top: 16px" />

    <n-modal v-model:show="formModalVisible" preset="card" :title="formTitle" style="width: 620px">
      <n-form :model="formModel" label-placement="left" label-width="90">
        <n-form-item label="设备名称" required>
          <n-input v-model:value="formModel.deviceName" placeholder="请输入设备名称" />
        </n-form-item>
        <n-form-item label="设备类型" required>
          <n-select v-model:value="formModel.deviceType" :options="deviceTypeOptions" placeholder="请选择设备类型" />
        </n-form-item>
        <n-form-item label="厂商" required>
          <n-input v-model:value="formModel.vendor" placeholder="请输入厂商" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="formModel.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </n-form-item>
      </n-form>

      <template #action>
        <n-space>
          <n-button @click="formModalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">确认</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="importModalVisible" preset="card" title="Excel导入设备" style="width: 760px">
      <n-space vertical :size="12">
        <n-card size="small" title="导入说明" :bordered="false">
          <div>1. 先下载模板并按列填写数据。</div>
          <div>2. 模板仅包含：设备名称、设备类型、厂商、描述（其中描述选填）。</div>
          <div>3. 设备ID、DID、注册时间等字段由后端自动生成；支持 .xlsx / .xls。</div>
        </n-card>

        <n-space align="center">
          <n-button :loading="downloadingTemplate" @click="handleDownloadTemplate">下载模板</n-button>
          <n-button :disabled="importing" @click="openImportPicker">选择文件</n-button>
          <span>{{ selectedImportFile?.name || '未选择文件' }}</span>
        </n-space>

        <n-card v-if="importTask" size="small" title="导入进度" :bordered="false">
          <n-space vertical :size="8">
            <n-progress type="line" :percentage="importProgressPercent" :processing="isImportTaskRunning" />
            <n-space justify="space-between">
              <span>{{ importProgressText }}</span>
              <span>{{ importTask.processedRows || 0 }}/{{ importTask.totalRows || 0 }}</span>
            </n-space>
            <div v-if="importTask.message && importTask.status === 'FAILED'" style="color: #d03050">
              {{ importTask.message }}
            </div>
          </n-space>
        </n-card>

        <n-card v-if="importResult" size="small" title="导入结果" :bordered="false">
          <n-descriptions bordered :column="3" size="small">
            <n-descriptions-item label="总行数">{{ importResult.totalRows || 0 }}</n-descriptions-item>
            <n-descriptions-item label="成功">{{ importResult.successCount || 0 }}</n-descriptions-item>
            <n-descriptions-item label="失败">{{ importResult.failedCount || 0 }}</n-descriptions-item>
          </n-descriptions>

          <n-empty
            v-if="!importResult.failures?.length"
            description="全部导入成功"
            style="margin-top: 12px"
          />
          <pre v-else class="json-viewer" style="margin-top: 12px">{{ importFailureText }}</pre>
        </n-card>
      </n-space>

      <template #action>
        <n-space>
          <n-button :disabled="importing" @click="importModalVisible = false">关闭</n-button>
          <n-button type="primary" :loading="importing" @click="handleConfirmImport">开始导入</n-button>
        </n-space>
      </template>
    </n-modal>

    <input
      ref="importInputRef"
      type="file"
      accept=".xlsx,.xls"
      style="display: none"
      @change="handleImportFileChange"
    />
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { DataTableColumns } from 'naive-ui';
import { useMessage } from 'naive-ui';
import {
  activateDevice,
  createDevice,
  downloadDeviceImportTemplate,
  getDeviceList,
  queryDeviceImportTask,
  startDeviceImportTask,
  updateDevice,
  updateDeviceStatus,
  type DeviceImportResult,
  type DeviceImportTask,
  type DevicePayload,
  type DeviceRecord
} from '@/api/device';
import PageContainer from '@/components/PageContainer.vue';
import SearchBar from '@/components/SearchBar.vue';
import StatusTag from '@/components/StatusTag.vue';
import TableActions from '@/components/TableActions.vue';
import { useDeviceFilterStore } from '@/stores/deviceFilter';
import { useUserStore } from '@/stores/user';
import {
  canActivateDevice,
  canCreateDevice,
  canEditDevice,
  canFreezeDevice,
  canImportDevice,
  canRevokeDevice
} from '@/utils/role';

const message = useMessage();
const router = useRouter();
const filterStore = useDeviceFilterStore();
const userStore = useUserStore();

const loading = ref(false);
const submitting = ref(false);
const importing = ref(false);
const downloadingTemplate = ref(false);
const tableData = ref<DeviceRecord[]>([]);
const statusActionLoadingMap = reactive<Record<string, boolean>>({});
const importInputRef = ref<HTMLInputElement | null>(null);
const importModalVisible = ref(false);
const importResult = ref<DeviceImportResult | null>(null);
const selectedImportFile = ref<File | null>(null);
const importTask = ref<DeviceImportTask | null>(null);
const importTaskTimer = ref<number | null>(null);

const queryForm = reactive({
  deviceId: filterStore.deviceId,
  deviceName: filterStore.deviceName,
  status: filterStore.status
});

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
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize;
    pagination.page = 1;
    fetchList();
  }
}));

const statusOptions = [
  { label: '已注册', value: '已注册' },
  { label: '已激活', value: '已激活' },
  { label: '已冻结', value: '已冻结' },
  { label: '已注销', value: '已注销' }
];

const deviceTypeOptions = [
  { label: '工业传感器', value: '工业传感器' },
  { label: '边缘网关', value: '边缘网关' },
  { label: '智能摄像头', value: '智能摄像头' },
  { label: '环境采集终端', value: '环境采集终端' }
];

const formModalVisible = ref(false);
const isEdit = ref(false);
const currentDeviceId = ref('');

const formModel = reactive<DevicePayload>({
  deviceName: '',
  deviceType: '',
  vendor: '',
  description: ''
});

const formTitle = computed(() => (isEdit.value ? '编辑设备' : '新增设备'));
const canCreate = computed(() => canCreateDevice(userStore.permissions));
const canImport = computed(() => canImportDevice(userStore.permissions));
const canEdit = computed(() => canEditDevice(userStore.permissions));
const canActivate = computed(() => canActivateDevice(userStore.permissions));
const canFreeze = computed(() => canFreezeDevice(userStore.permissions));
const canRevoke = computed(() => canRevokeDevice(userStore.permissions));

const importFailureText = computed(() => {
  if (!importResult.value?.failures?.length) {
    return '';
  }
  return importResult.value.failures
    .map((item) => {
      const rowText = `第${item.rowNumber}行`;
      const deviceText = [item.deviceId ? `deviceId=${item.deviceId}` : '', item.deviceName ? `设备名=${item.deviceName}` : '']
        .filter(Boolean)
        .join('，');
      return `${rowText}${deviceText ? `（${deviceText}）` : ''}: ${item.message}`;
    })
    .join('\n');
});

const isImportTaskRunning = computed(() => ['PENDING', 'RUNNING'].includes(importTask.value?.status || ''));

const importProgressPercent = computed(() => {
  if (!importTask.value) {
    return 0;
  }
  return importTask.value.progressPercent || 0;
});

const importProgressText = computed(() => {
  if (!importTask.value) {
    return '';
  }
  const total = importTask.value.totalRows || 0;
  const processed = importTask.value.processedRows || 0;
  if (importTask.value.status === 'PENDING') {
    return '任务已提交，等待执行';
  }
  if (importTask.value.status === 'RUNNING' && importTask.value.stage === 'PARSING') {
    return '正在解析 Excel';
  }
  if (importTask.value.status === 'RUNNING') {
    return `正在导入 ${processed}/${total || '?'}`;
  }
  if (importTask.value.status === 'SUCCESS') {
    return `导入完成：成功 ${importTask.value.successCount || 0}，失败 ${importTask.value.failedCount || 0}`;
  }
  return importTask.value.message || '导入任务失败';
});

const resetForm = () => {
  formModel.deviceName = '';
  formModel.deviceType = '';
  formModel.vendor = '';
  formModel.description = '';
};

const openCreateModal = () => {
  isEdit.value = false;
  currentDeviceId.value = '';
  resetForm();
  formModalVisible.value = true;
};

const openEditModal = (row: DeviceRecord) => {
  isEdit.value = true;
  currentDeviceId.value = row.deviceId;
  formModel.deviceName = row.deviceName;
  formModel.deviceType = row.deviceType;
  formModel.vendor = row.vendor;
  formModel.description = row.description;
  formModalVisible.value = true;
};

const fetchList = async () => {
  loading.value = true;
  try {
    filterStore.setFilter({ ...queryForm });
    const res = await getDeviceList({
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...queryForm
    });
    tableData.value = res.data;
    pagination.itemCount = res.total || 0;
  } catch (error: any) {
    message.error(error?.message || '设备列表加载失败');
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
  queryForm.deviceName = '';
  queryForm.status = '';
  filterStore.resetFilter();
  pagination.page = 1;
  fetchList();
};

const stopImportTaskPolling = () => {
  if (importTaskTimer.value !== null) {
    window.clearInterval(importTaskTimer.value);
    importTaskTimer.value = null;
  }
};

const pollImportTaskStatus = async (silent = false) => {
  if (!importTask.value?.taskId) {
    return;
  }
  try {
    const res = await queryDeviceImportTask(importTask.value.taskId);
    importTask.value = res.data;
    importing.value = ['PENDING', 'RUNNING'].includes(res.data.status || '');

    if (res.data.status === 'SUCCESS') {
      stopImportTaskPolling();
      importing.value = false;
      importResult.value = res.data.result || null;
      const successCount = res.data.successCount ?? res.data.result?.successCount ?? 0;
      const failedCount = res.data.failedCount ?? res.data.result?.failedCount ?? 0;
      if (failedCount > 0) {
        message.warning(`导入完成：成功 ${successCount} 条，失败 ${failedCount} 条`);
      } else {
        message.success(`导入完成：成功 ${successCount} 条`);
      }
      await fetchList();
      return;
    }

    if (res.data.status === 'FAILED') {
      stopImportTaskPolling();
      importing.value = false;
      importResult.value = res.data.result || null;
      message.error(res.data.message || 'Excel 导入失败');
      await fetchList();
    }
  } catch (error: any) {
    if (silent) {
      return;
    }
    message.error(error?.message || '导入进度查询失败');
    stopImportTaskPolling();
    importing.value = false;
  }
};

const startImportTaskPolling = () => {
  stopImportTaskPolling();
  importTaskTimer.value = window.setInterval(() => {
    void pollImportTaskStatus(true);
  }, 1000);
};

const openImportModal = () => {
  if (!isImportTaskRunning.value) {
    selectedImportFile.value = null;
    importResult.value = null;
    importTask.value = null;
  }
  importModalVisible.value = true;
};

const openImportPicker = () => {
  if (importing.value || downloadingTemplate.value) {
    return;
  }
  importInputRef.value?.click();
};

const handleImportFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }
  if (!/\.(xlsx|xls)$/i.test(file.name)) {
    message.warning('请上传 xlsx 或 xls 文件');
    return;
  }
  selectedImportFile.value = file;
};

const handleDownloadTemplate = async () => {
  downloadingTemplate.value = true;
  try {
    const response = await downloadDeviceImportTemplate();
    const blob = response.data;
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'device-import-template.xlsx';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error: any) {
    message.error(error?.message || '模板下载失败');
  } finally {
    downloadingTemplate.value = false;
  }
};

const handleConfirmImport = async () => {
  if (!selectedImportFile.value) {
    message.warning('请先选择要导入的 Excel 文件');
    return;
  }
  if (importing.value) {
    return;
  }

  importing.value = true;
  importResult.value = null;
  importTask.value = null;
  stopImportTaskPolling();
  try {
    const res = await startDeviceImportTask(selectedImportFile.value);
    importTask.value = res.data;
    message.info('导入任务已提交，正在后台处理');
    await pollImportTaskStatus();
    if (isImportTaskRunning.value) {
      startImportTaskPolling();
    }
  } catch (error: any) {
    importing.value = false;
    message.error(error?.message || '导入任务提交失败');
  }
};

const handleSubmit = async () => {
  if (!formModel.deviceName || !formModel.deviceType || !formModel.vendor) {
    message.warning('请完整填写设备名称、设备类型与厂商');
    return;
  }

  submitting.value = true;
  try {
    if (isEdit.value) {
      await updateDevice(currentDeviceId.value, { ...formModel });
      message.success('设备信息更新成功');
    } else {
      await createDevice({ ...formModel });
      message.success('设备新增成功');
    }
    formModalVisible.value = false;
    fetchList();
  } catch (error: any) {
    message.error(error?.message || '保存失败');
  } finally {
    submitting.value = false;
  }
};

const buildStatusActionKey = (deviceId: string, action: string) => `${deviceId}:${action}`;

const setStatusActionLoading = (deviceId: string, action: string) => {
  statusActionLoadingMap[buildStatusActionKey(deviceId, action)] = true;
};

const clearStatusActionLoading = (deviceId: string, action: string) => {
  delete statusActionLoadingMap[buildStatusActionKey(deviceId, action)];
};

const isRowActionLoading = (row: DeviceRecord, action: string) => Boolean(statusActionLoadingMap[buildStatusActionKey(row.deviceId, action)]);

const isRowAnyActionLoading = (row: DeviceRecord) =>
  isRowActionLoading(row, 'activate') || isRowActionLoading(row, 'freeze') || isRowActionLoading(row, 'revoke');

const handleStatusUpdate = async (row: DeviceRecord, status: string, action: string) => {
  if (isRowAnyActionLoading(row)) {
    return;
  }
  setStatusActionLoading(row.deviceId, action);
  try {
    await updateDeviceStatus(row.deviceId, status);
    message.success(`设备状态已更新为${status}`);
    await fetchList();
  } catch (error: any) {
    message.error(error?.message || '状态更新失败');
  } finally {
    clearStatusActionLoading(row.deviceId, action);
  }
};

const handleActivate = async (row: DeviceRecord) => {
  if (isRowAnyActionLoading(row)) {
    return;
  }
  setStatusActionLoading(row.deviceId, 'activate');
  try {
    await activateDevice(row.deviceId);
    message.success('设备状态已更新为已激活');
    await fetchList();
  } catch (error: any) {
    message.error(error?.message || '状态更新失败');
  } finally {
    clearStatusActionLoading(row.deviceId, 'activate');
  }
};

const handleRowAction = (action: string, row: DeviceRecord) => {
  if (action === 'detail') {
    router.push(`/device/${row.deviceId}`);
    return;
  }
  if (action === 'edit') {
    openEditModal(row);
    return;
  }
  if (action === 'activate') {
    handleActivate(row);
    return;
  }
  if (action === 'freeze') {
    handleStatusUpdate(row, '已冻结', 'freeze');
    return;
  }
  if (action === 'revoke') {
    handleStatusUpdate(row, '已注销', 'revoke');
  }
};

const columns: DataTableColumns<DeviceRecord> = [
  { title: '设备ID', key: 'deviceId', width: 120 },
  { title: '设备名称', key: 'deviceName', width: 160 },
  { title: 'DID标识', key: 'did', width: 220, ellipsis: { tooltip: true } },
  { title: '设备类型', key: 'deviceType', width: 120 },
  { title: '厂商', key: 'vendor', width: 120 },
  {
    title: '当前状态',
    key: 'lifecycleStatus',
    width: 100,
    render: (row) => h(StatusTag, { status: row.lifecycleStatus })
  },
  { title: '注册时间', key: 'registerTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render: (row) =>
      h(TableActions, {
        actions: [
          { key: 'detail', label: '查看详情' },
          ...(canEdit.value ? [{ key: 'edit', label: '编辑设备', type: 'default' as const }] : []),
          ...(canActivate.value && ['已注册', '已冻结'].includes(row.lifecycleStatus)
            ? [{
                key: 'activate',
                label: '激活设备',
                type: 'primary' as const,
                loading: isRowActionLoading(row, 'activate'),
                disabled: isRowAnyActionLoading(row)
              }]
            : []),
          ...(canFreeze.value
            ? [{
                key: 'freeze',
                label: '冻结设备',
                type: 'warning' as const,
                loading: isRowActionLoading(row, 'freeze'),
                disabled: row.lifecycleStatus !== '已激活' || isRowAnyActionLoading(row)
              }]
            : []),
          ...(canRevoke.value
            ? [{
                key: 'revoke',
                label: '注销设备',
                type: 'error' as const,
                loading: isRowActionLoading(row, 'revoke'),
                disabled: row.lifecycleStatus === '已注销' || isRowAnyActionLoading(row)
              }]
            : [])
        ],
        onAction: (key: string) => handleRowAction(key, row)
      })
  }
];

onMounted(fetchList);
onUnmounted(() => {
  stopImportTaskPolling();
});
</script>
