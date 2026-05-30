<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>用户管理</h1>
        <p>维护后台用户、角色、状态和登录密码。</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新增用户
      </ElButton>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElInput
          v-model.trim="queryForm.keyword"
          class="filter-keyword"
          clearable
          placeholder="用户名 / 邮箱 / 手机"
          @keyup.enter="loadList"
        />
        <ElSelect v-model="queryForm.role" clearable placeholder="角色">
          <ElOption v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
        <ElSelect v-model="queryForm.status" clearable placeholder="状态">
          <ElOption
            v-for="option in masterStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
        <ElButton type="primary" @click="handleSearch">
          <ElIcon><Search /></ElIcon>
          查询
        </ElButton>
        <ElButton @click="handleReset">
          <ElIcon><Refresh /></ElIcon>
          重置
        </ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="records" row-key="id">
          <ElTableColumn prop="id" label="ID" width="80" />
          <ElTableColumn prop="username" label="用户名" min-width="140" show-overflow-tooltip />
          <ElTableColumn label="角色" width="110">
            <template #default="{ row }">
              <ElTag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="plain">
                {{ roleLabel(row.role) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <ElTag :type="statusType(masterStatusOptions, row.status)" effect="plain">
                {{ statusLabel(masterStatusOptions, row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
          <ElTableColumn prop="phone" label="手机" min-width="130" show-overflow-tooltip />
          <ElTableColumn label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="340">
            <template #default="{ row }">
              <span class="table-actions">
                <ElButton size="small" type="primary" link @click="openEdit(row)">编辑</ElButton>
                <ElButton size="small" type="primary" link @click="openPasswordDialog(row)">重置密码</ElButton>
                <ElButton size="small" type="primary" link @click="openRoleDialog(row)">角色</ElButton>
                <ElButton size="small" type="primary" link @click="openWarehouseDialog(row)">仓库</ElButton>
                <ElButton
                  size="small"
                  :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                  link
                  :disabled="isSelf(row)"
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                </ElButton>
                <ElButton size="small" type="danger" link :disabled="isSelf(row)" @click="removeUser(row)">
                  删除
                </ElButton>
              </span>
            </template>
          </ElTableColumn>
        </ElTable>

        <div class="pagination-row">
          <ElPagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[10, 20, 50]"
            :total="pagination.total"
            @current-change="loadList"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <ElDialog v-model="formDialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="680px" destroy-on-close>
      <ElForm ref="formRef" :model="form" :rules="formRules" label-width="86px">
        <div class="form-grid">
          <ElFormItem label="用户名" prop="username">
            <ElInput v-model.trim="form.username" maxlength="50" placeholder="请输入用户名" />
          </ElFormItem>
          <ElFormItem v-if="!editingId" label="密码" prop="password">
            <ElInput v-model="form.password" maxlength="100" placeholder="请输入密码" show-password type="password" />
          </ElFormItem>
          <ElFormItem label="角色" prop="role">
            <ElSelect v-model="form.role" placeholder="请选择角色">
              <ElOption v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="状态" prop="status">
            <ElSelect v-model="form.status" placeholder="请选择状态">
              <ElOption
                v-for="option in masterStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="邮箱" prop="email">
            <ElInput v-model.trim="form.email" maxlength="100" placeholder="请输入邮箱" />
          </ElFormItem>
          <ElFormItem label="手机" prop="phone">
            <ElInput v-model.trim="form.phone" maxlength="30" placeholder="请输入手机号" />
          </ElFormItem>
        </div>
      </ElForm>
      <template #footer>
        <ElButton @click="formDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="saveUser">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="passwordDialogVisible" title="重置密码" width="460px" destroy-on-close>
      <ElForm ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="92px">
        <ElFormItem label="新密码" prop="password">
          <ElInput v-model="passwordForm.password" maxlength="100" show-password type="password" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="passwordDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="savingPassword" @click="savePassword">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="roleDialogVisible" title="分配角色" width="520px" destroy-on-close>
      <div class="assign-target">用户：{{ selectedUser?.username }}</div>
      <ElCheckboxGroup v-model="roleForm.roleIds" class="assign-options">
        <ElCheckbox v-for="role in availableRoles" :key="role.id" :label="role.id">
          {{ role.name }}
          <span class="assign-code">{{ role.code }}</span>
        </ElCheckbox>
      </ElCheckboxGroup>
      <template #footer>
        <ElButton @click="roleDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="savingRoles" @click="saveUserRoles">保存角色</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="warehouseDialogVisible" title="分配仓库范围" width="560px" destroy-on-close>
      <div class="assign-target">用户：{{ selectedUser?.username }}</div>
      <p class="assign-help">不选择仓库时表示不限制仓库范围，后续数据权限阶段会继续细化。</p>
      <ElCheckboxGroup v-model="warehouseForm.warehouseIds" class="assign-options">
        <ElCheckbox v-for="warehouse in availableWarehouses" :key="warehouse.id" :label="warehouse.id">
          {{ warehouse.name }}
          <span class="assign-code">{{ warehouse.code }}</span>
        </ElCheckbox>
      </ElCheckboxGroup>
      <template #footer>
        <ElButton @click="warehouseDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="savingWarehouses" @click="saveUserWarehouses">保存仓库范围</ElButton>
      </template>
    </ElDialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import {
  createUser,
  deleteUser,
  getUserRoles,
  getUserWarehouses,
  pageUsers,
  updateUser,
  updateUserPassword,
  updateUserRoles,
  updateUserStatus,
  updateUserWarehouses
} from '@/api/users'
import { roleApi } from '@/api/rbac'
import { warehouseApi } from '@/api/business'
import { masterStatusOptions, statusLabel, statusType } from '@/constants/options'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'

const roleOptions = ref([
  {
    label: '管理员',
    value: 'ADMIN'
  },
  {
    label: '仓库主管',
    value: 'MANAGER'
  },
  {
    label: '仓库人员',
    value: 'STAFF'
  },
  {
    label: '只读查看员',
    value: 'VIEWER'
  }
])

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const savingPassword = ref(false)
const savingRoles = ref(false)
const savingWarehouses = ref(false)
const formDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const warehouseDialogVisible = ref(false)
const editingId = ref(null)
const passwordUserId = ref(null)
const selectedUser = ref(null)
const formRef = ref()
const passwordFormRef = ref()
const records = ref([])
const availableRoles = ref([])
const availableWarehouses = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  keyword: '',
  role: '',
  status: ''
})
const form = reactive({
  username: '',
  password: '',
  role: 'STAFF',
  status: 'ACTIVE',
  email: '',
  phone: ''
})
const passwordForm = reactive({
  password: ''
})
const roleForm = reactive({
  roleIds: []
})
const warehouseForm = reactive({
  warehouseIds: []
})

const formRules = computed(() => ({
  username: [
    {
      required: true,
      message: '请输入用户名',
      trigger: 'blur'
    }
  ],
  password: editingId.value
    ? []
    : [
        {
          required: true,
          message: '请输入密码',
          trigger: 'blur'
        },
        {
          min: 6,
          message: '密码至少 6 位',
          trigger: 'blur'
        }
      ],
  role: [
    {
      required: true,
      message: '请选择角色',
      trigger: 'change'
    }
  ],
  status: [
    {
      required: true,
      message: '请选择状态',
      trigger: 'change'
    }
  ],
  email: [
    {
      type: 'email',
      message: '邮箱格式不正确',
      trigger: 'blur'
    }
  ]
}))
const passwordRules = {
  password: [
    {
      required: true,
      message: '请输入新密码',
      trigger: 'blur'
    },
    {
      min: 6,
      message: '密码至少 6 位',
      trigger: 'blur'
    }
  ]
}

function roleLabel(role) {
  return roleOptions.value.find((option) => option.value === role)?.label || role || '-'
}

function isSelf(row) {
  return row.id === authStore.user?.id
}

function resetForm(row = {}) {
  form.username = row.username || ''
  form.password = ''
  form.role = row.role || 'STAFF'
  form.status = row.status || 'ACTIVE'
  form.email = row.email || ''
  form.phone = row.phone || ''
}

async function loadList() {
  loading.value = true

  try {
    const result = await pageUsers({
      page: pagination.page,
      size: pagination.size,
      keyword: queryForm.keyword,
      role: queryForm.role,
      status: queryForm.status
    })

    records.value = result.records || []
    pagination.total = result.total || 0
  } finally {
    loading.value = false
  }
}

async function loadRoleOptions() {
  const result = await roleApi.page({
    page: 1,
    size: 100,
    status: 'ACTIVE'
  })
  availableRoles.value = result.records || []
  if (availableRoles.value.length > 0) {
    roleOptions.value = availableRoles.value.map((role) => ({
      label: role.name,
      value: role.code
    }))
  }
}

async function loadWarehouseOptions() {
  const result = await warehouseApi.page({
    page: 1,
    size: 100,
    status: 'ACTIVE'
  })
  availableWarehouses.value = result.records || []
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.role = ''
  queryForm.status = ''
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

function openCreate() {
  editingId.value = null
  resetForm()
  formDialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  resetForm(row)
  formDialogVisible.value = true
}

async function saveUser() {
  await formRef.value?.validate()

  if (editingId.value === authStore.user?.id && form.status === 'DISABLED') {
    ElMessage.warning('不能禁用当前登录用户')
    return
  }

  saving.value = true

  try {
    const payload = {
      username: form.username,
      role: form.role,
      status: form.status,
      email: form.email,
      phone: form.phone
    }

    if (editingId.value) {
      await updateUser(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createUser({
        ...payload,
        password: form.password
      })
      ElMessage.success('新增成功')
    }

    formDialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

function openPasswordDialog(row) {
  passwordUserId.value = row.id
  passwordForm.password = ''
  passwordDialogVisible.value = true
}

async function openRoleDialog(row) {
  selectedUser.value = row
  roleDialogVisible.value = true
  await loadRoleOptions()
  const roles = await getUserRoles(row.id)
  roleForm.roleIds = (roles || []).map((role) => role.id)
}

async function saveUserRoles() {
  if (roleForm.roleIds.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }

  savingRoles.value = true
  try {
    await updateUserRoles(selectedUser.value.id, roleForm.roleIds)
    ElMessage.success('角色保存成功')
    roleDialogVisible.value = false
    loadList()
  } finally {
    savingRoles.value = false
  }
}

async function openWarehouseDialog(row) {
  selectedUser.value = row
  warehouseDialogVisible.value = true
  await loadWarehouseOptions()
  const warehouses = await getUserWarehouses(row.id)
  warehouseForm.warehouseIds = (warehouses || []).map((warehouse) => warehouse.id)
}

async function saveUserWarehouses() {
  savingWarehouses.value = true
  try {
    await updateUserWarehouses(selectedUser.value.id, warehouseForm.warehouseIds)
    ElMessage.success('仓库范围保存成功')
    warehouseDialogVisible.value = false
  } finally {
    savingWarehouses.value = false
  }
}

async function savePassword() {
  await passwordFormRef.value?.validate()
  savingPassword.value = true

  try {
    await updateUserPassword(passwordUserId.value, {
      password: passwordForm.password
    })
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } finally {
    savingPassword.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionText = nextStatus === 'ACTIVE' ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确认${actionText}用户「${row.username}」？`, `${actionText}用户`, {
      type: 'warning',
      confirmButtonText: actionText,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await updateUserStatus(row.id, {
    status: nextStatus
  })
  ElMessage.success(`${actionText}成功`)
  loadList()
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => {
  loadRoleOptions()
  loadList()
})
</script>

<style scoped>
.assign-target {
  margin-bottom: 12px;
  color: var(--wms-ink);
  font-weight: 650;
}

.assign-help {
  margin: 0 0 12px;
  color: var(--wms-muted);
  font-size: 13px;
}

.assign-options {
  display: grid;
  max-height: 360px;
  gap: 10px;
  overflow: auto;
}

.assign-code {
  margin-left: 6px;
  color: var(--wms-muted);
  font-size: 12px;
}
</style>
