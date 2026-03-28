<template>
  <page-container title="用户管理" description="新增、编辑、删除用户并分配角色">
    <template #extra>
      <n-button type="primary" @click="openCreateModal">新增用户</n-button>
    </template>

    <n-data-table
      :columns="columns"
      :data="users"
      :loading="loading"
      :pagination="tablePagination"
      :bordered="false"
      remote
    />

    <n-empty v-if="!loading && !users.length" description="暂无用户数据" style="margin-top: 12px" />

    <n-modal v-model:show="formVisible" preset="card" :title="formTitle" style="width: 520px">
      <n-form :model="formModel" label-placement="left" label-width="90">
        <n-form-item label="用户名" required>
          <n-input v-model:value="formModel.username" placeholder="请输入用户名" :disabled="isEdit" />
        </n-form-item>
        <n-form-item :label="isEdit ? '新密码' : '密码'" :required="!isEdit">
          <n-input
            v-model:value="formModel.password"
            type="password"
            show-password-on="click"
            :placeholder="isEdit ? '不修改请留空' : '请输入登录密码'"
          />
        </n-form-item>
        <n-form-item label="角色" required>
          <n-select v-model:value="formModel.roleId" :options="roleOptions" placeholder="请选择角色" />
        </n-form-item>
        <n-form-item label="状态" required>
          <n-select v-model:value="formModel.statusCode" :options="statusOptions" placeholder="请选择状态" />
        </n-form-item>
      </n-form>

      <template #action>
        <n-space>
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useDialog, useMessage, type DataTableColumns } from 'naive-ui';
import {
  createPermissionUser,
  deletePermissionUser,
  getPermissionRoles,
  getPermissionUsers,
  updatePermissionUser,
  type PermissionRole,
  type PermissionUser
} from '@/api/permission';
import PageContainer from '@/components/PageContainer.vue';
import StatusTag from '@/components/StatusTag.vue';
import TableActions from '@/components/TableActions.vue';

const message = useMessage();
const dialog = useDialog();

const users = ref<PermissionUser[]>([]);
const roleList = ref<PermissionRole[]>([]);
const loading = ref(false);
const submitting = ref(false);

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const tablePagination = computed(() => ({
  page: pagination.page,
  pageSize: pagination.pageSize,
  itemCount: pagination.total,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onUpdatePage: (page: number) => {
    pagination.page = page;
    fetchUsers();
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize;
    pagination.page = 1;
    fetchUsers();
  }
}));

const isEdit = ref(false);
const currentUserId = ref<number | null>(null);
const formVisible = ref(false);
const formModel = reactive({
  username: '',
  password: '',
  roleId: null as number | null,
  statusCode: 'ENABLED' as 'ENABLED' | 'DISABLED'
});

const formTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'));

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '禁用', value: 'DISABLED' }
];

const roleOptions = computed(() =>
  roleList.value.map((item) => ({
    label: item.roleName,
    value: item.id
  }))
);

const columns: DataTableColumns<PermissionUser> = [
  { title: '用户名', key: 'username' },
  { title: '角色', key: 'role' },
  {
    title: '状态',
    key: 'status',
    render: (row) => h(StatusTag, { status: row.status })
  },
  { title: '创建时间', key: 'createdAt' },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row) =>
      h(TableActions, {
        actions: [
          { key: 'edit', label: '编辑', type: 'primary' as const },
          { key: 'delete', label: '删除', type: 'error' as const }
        ],
        onAction: (key: string) => handleAction(key, row)
      })
  }
];

const fetchUsers = async () => {
  loading.value = true;
  try {
    const res = await getPermissionUsers({
      page: pagination.page,
      pageSize: pagination.pageSize
    });
    users.value = res.data;
    pagination.total = res.total || 0;
  } catch (error: any) {
    message.error(error?.message || '用户列表加载失败');
  } finally {
    loading.value = false;
  }
};

const fetchRoles = async () => {
  try {
    const res = await getPermissionRoles({ page: 1, pageSize: 200 });
    roleList.value = res.data;
  } catch (error: any) {
    message.error(error?.message || '角色数据加载失败');
  }
};

const resetForm = () => {
  currentUserId.value = null;
  formModel.username = '';
  formModel.password = '';
  formModel.roleId = null;
  formModel.statusCode = 'ENABLED';
};

const openCreateModal = () => {
  isEdit.value = false;
  resetForm();
  formVisible.value = true;
};

const openEditModal = (row: PermissionUser) => {
  isEdit.value = true;
  currentUserId.value = row.id;
  formModel.username = row.username;
  formModel.password = '';
  formModel.roleId = row.roleId;
  formModel.statusCode = row.statusCode === 'DISABLED' ? 'DISABLED' : 'ENABLED';
  formVisible.value = true;
};

const handleSubmit = async () => {
  if (!formModel.username) {
    message.warning('请输入用户名');
    return;
  }
  if (!isEdit.value && !formModel.password) {
    message.warning('请输入密码');
    return;
  }
  if (!formModel.roleId) {
    message.warning('请选择角色');
    return;
  }

  submitting.value = true;
  try {
    if (isEdit.value && currentUserId.value) {
      await updatePermissionUser(currentUserId.value, {
        roleId: formModel.roleId,
        status: formModel.statusCode,
        password: formModel.password || undefined
      });
      message.success('用户更新成功');
    } else {
      await createPermissionUser({
        username: formModel.username,
        password: formModel.password,
        roleId: formModel.roleId,
        status: formModel.statusCode
      });
      message.success('用户创建成功');
    }
    formVisible.value = false;
    await fetchUsers();
  } catch (error: any) {
    message.error(error?.message || '保存失败');
  } finally {
    submitting.value = false;
  }
};

const deleteUser = (row: PermissionUser) => {
  dialog.warning({
    title: '删除用户',
    content: `确认删除用户 ${row.username} 吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deletePermissionUser(row.id);
        message.success('用户删除成功');
        if (users.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1;
        }
        await fetchUsers();
      } catch (error: any) {
        message.error(error?.message || '删除失败');
      }
    }
  });
};

const handleAction = (action: string, row: PermissionUser) => {
  if (action === 'edit') {
    openEditModal(row);
    return;
  }
  if (action === 'delete') {
    deleteUser(row);
  }
};

onMounted(async () => {
  await fetchRoles();
  await fetchUsers();
});
</script>
