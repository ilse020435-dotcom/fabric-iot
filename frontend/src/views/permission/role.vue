<template>
  <page-container title="角色管理" description="新增、编辑、删除角色，并配置菜单和按钮权限">
    <template #extra>
      <n-button type="primary" @click="openCreateModal">新增角色</n-button>
    </template>

    <n-data-table
      :columns="columns"
      :data="roles"
      :loading="loading"
      :pagination="tablePagination"
      :bordered="false"
      remote
    />

    <n-empty v-if="!loading && !roles.length" description="暂无角色数据" style="margin-top: 12px" />

    <n-modal v-model:show="formVisible" preset="card" :title="formTitle" style="width: 560px">
      <n-form :model="formModel" label-placement="left" label-width="90">
        <n-form-item label="角色编码" required>
          <n-input v-model:value="formModel.roleCode" placeholder="例如：OPERATOR" />
        </n-form-item>
        <n-form-item label="角色名称" required>
          <n-input v-model:value="formModel.roleName" placeholder="请输入角色名称" />
        </n-form-item>
        <n-form-item label="角色描述">
          <n-input v-model:value="formModel.roleDesc" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space>
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal
      v-model:show="permissionVisible"
      preset="card"
      :title="`权限配置 - ${currentRoleName || ''}`"
      style="width: 680px"
    >
      <n-spin :show="treeLoading || permissionLoading">
        <n-tree
          checkable
          block-line
          default-expand-all
          v-model:checked-keys="checkedPermissionKeys"
          :data="permissionTreeData"
        />
      </n-spin>
      <n-empty
        v-if="!treeLoading && !permissionLoading && !permissionTreeData.length"
        description="暂无权限树数据"
        style="margin-top: 12px"
      />
      <template #action>
        <n-space>
          <n-button @click="permissionVisible = false">取消</n-button>
          <n-button type="primary" :loading="permissionSubmitting" @click="savePermissions">保存权限</n-button>
        </n-space>
      </template>
    </n-modal>
  </page-container>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useDialog, useMessage, type DataTableColumns } from 'naive-ui';
import {
  createPermissionRole,
  deletePermissionRole,
  getPermissionRoles,
  getPermissionTree,
  getRolePermissionCodes,
  saveRolePermissionCodes,
  updatePermissionRole,
  type PermissionRole,
  type PermissionTreeNode
} from '@/api/permission';
import PageContainer from '@/components/PageContainer.vue';
import TableActions from '@/components/TableActions.vue';

const message = useMessage();
const dialog = useDialog();

const roles = ref<PermissionRole[]>([]);
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
    fetchRoles();
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize;
    pagination.page = 1;
    fetchRoles();
  }
}));

const isEdit = ref(false);
const currentRoleId = ref<number | null>(null);
const formVisible = ref(false);
const formModel = reactive({
  roleCode: '',
  roleName: '',
  roleDesc: ''
});

const permissionVisible = ref(false);
const permissionLoading = ref(false);
const permissionSubmitting = ref(false);
const treeLoading = ref(false);
const currentRoleName = ref('');
const permissionTree = ref<PermissionTreeNode[]>([]);
const checkedPermissionKeys = ref<string[]>([]);

const formTitle = computed(() => (isEdit.value ? '编辑角色' : '新增角色'));

const permissionTypeLabel: Record<string, string> = {
  MENU: '菜单',
  BUTTON: '按钮',
  API: '接口'
};

const permissionTreeData = computed(() => {
  const mapNode = (node: PermissionTreeNode): PermissionTreeNode => ({
    ...node,
    label: `${node.label}（${permissionTypeLabel[node.type] || node.type}）`,
    children: node.children?.map(mapNode)
  });
  return permissionTree.value.map(mapNode);
});

const columns: DataTableColumns<PermissionRole> = [
  { title: '角色名称', key: 'roleName' },
  { title: '角色编码', key: 'roleCode' },
  { title: '角色描述', key: 'roleDesc' },
  { title: '权限数量', key: 'permissionCount', width: 100 },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    render: (row) =>
      h(TableActions, {
        actions: [
          { key: 'edit', label: '编辑', type: 'primary' as const },
          { key: 'permission', label: '权限配置', type: 'warning' as const },
          { key: 'delete', label: '删除', type: 'error' as const }
        ],
        onAction: (key: string) => handleAction(key, row)
      })
  }
];

const fetchRoles = async () => {
  loading.value = true;
  try {
    const res = await getPermissionRoles({
      page: pagination.page,
      pageSize: pagination.pageSize
    });
    roles.value = res.data;
    pagination.total = res.total || 0;
  } catch (error: any) {
    message.error(error?.message || '角色列表加载失败');
  } finally {
    loading.value = false;
  }
};

const fetchPermissionTree = async () => {
  if (permissionTree.value.length) {
    return;
  }
  treeLoading.value = true;
  try {
    const res = await getPermissionTree();
    permissionTree.value = res.data;
  } catch (error: any) {
    message.error(error?.message || '权限树加载失败');
  } finally {
    treeLoading.value = false;
  }
};

const resetForm = () => {
  currentRoleId.value = null;
  formModel.roleCode = '';
  formModel.roleName = '';
  formModel.roleDesc = '';
};

const openCreateModal = () => {
  isEdit.value = false;
  resetForm();
  formVisible.value = true;
};

const openEditModal = (row: PermissionRole) => {
  isEdit.value = true;
  currentRoleId.value = row.id;
  formModel.roleCode = row.roleCode;
  formModel.roleName = row.roleName;
  formModel.roleDesc = row.roleDesc || '';
  formVisible.value = true;
};

const handleSubmit = async () => {
  if (!formModel.roleCode) {
    message.warning('请输入角色编码');
    return;
  }
  if (!formModel.roleName) {
    message.warning('请输入角色名称');
    return;
  }

  submitting.value = true;
  try {
    if (isEdit.value && currentRoleId.value) {
      await updatePermissionRole(currentRoleId.value, {
        roleCode: formModel.roleCode,
        roleName: formModel.roleName,
        roleDesc: formModel.roleDesc
      });
      message.success('角色更新成功');
    } else {
      await createPermissionRole({
        roleCode: formModel.roleCode,
        roleName: formModel.roleName,
        roleDesc: formModel.roleDesc
      });
      message.success('角色创建成功');
    }
    formVisible.value = false;
    await fetchRoles();
  } catch (error: any) {
    message.error(error?.message || '保存失败');
  } finally {
    submitting.value = false;
  }
};

const deleteRole = (row: PermissionRole) => {
  dialog.warning({
    title: '删除角色',
    content: `确认删除角色 ${row.roleName} 吗？`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deletePermissionRole(row.id);
        message.success('角色删除成功');
        if (roles.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1;
        }
        await fetchRoles();
      } catch (error: any) {
        message.error(error?.message || '删除失败');
      }
    }
  });
};

const openPermissionModal = async (row: PermissionRole) => {
  currentRoleId.value = row.id;
  currentRoleName.value = row.roleName;
  checkedPermissionKeys.value = [];
  permissionVisible.value = true;

  permissionLoading.value = true;
  try {
    await fetchPermissionTree();
    const res = await getRolePermissionCodes(row.id);
    checkedPermissionKeys.value = res.data;
  } catch (error: any) {
    message.error(error?.message || '角色权限加载失败');
  } finally {
    permissionLoading.value = false;
  }
};

const savePermissions = async () => {
  if (!currentRoleId.value) {
    return;
  }
  permissionSubmitting.value = true;
  try {
    await saveRolePermissionCodes(currentRoleId.value, {
      permissionCodes: checkedPermissionKeys.value
    });
    message.success('权限配置保存成功');
    permissionVisible.value = false;
    await fetchRoles();
  } catch (error: any) {
    message.error(error?.message || '权限保存失败');
  } finally {
    permissionSubmitting.value = false;
  }
};

const handleAction = (action: string, row: PermissionRole) => {
  if (action === 'edit') {
    openEditModal(row);
    return;
  }
  if (action === 'permission') {
    openPermissionModal(row);
    return;
  }
  if (action === 'delete') {
    deleteRole(row);
  }
};

onMounted(fetchRoles);
</script>
