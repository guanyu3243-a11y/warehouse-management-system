<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>角色管理</h1>
        <p>维护系统角色，并为角色配置菜单、按钮和接口权限。</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新增角色
      </ElButton>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElInput
          v-model.trim="queryForm.keyword"
          class="filter-keyword"
          clearable
          placeholder="角色编码 / 名称"
          @keyup.enter="loadList"
        />
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
          <ElTableColumn prop="code" label="角色编码" min-width="130" />
          <ElTableColumn prop="name" label="角色名称" min-width="140" />
          <ElTableColumn prop="description" label="说明" min-width="220" show-overflow-tooltip />
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <ElTag :type="statusType(masterStatusOptions, row.status)" effect="plain">
                {{ statusLabel(masterStatusOptions, row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="260">
            <template #default="{ row }">
              <span class="table-actions">
                <ElButton size="small" type="primary" link @click="openPermissionDialog(row)">权限</ElButton>
                <ElButton size="small" type="primary" link @click="openEdit(row)">编辑</ElButton>
                <ElButton size="small" :type="row.status === 'ACTIVE' ? 'warning' : 'success'" link @click="toggleStatus(row)">
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </ElButton>
                <ElButton size="small" type="danger" link :disabled="isBuiltIn(row)" @click="removeRole(row)">
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

    <ElDialog v-model="formDialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="620px" destroy-on-close>
      <ElForm ref="formRef" :model="form" :rules="formRules" label-width="86px">
        <ElFormItem label="角色编码" prop="code">
          <ElInput v-model.trim="form.code" :disabled="Boolean(editingId)" maxlength="50" placeholder="如 MANAGER" />
        </ElFormItem>
        <ElFormItem label="角色名称" prop="name">
          <ElInput v-model.trim="form.name" maxlength="100" placeholder="请输入角色名称" />
        </ElFormItem>
        <ElFormItem label="状态" prop="status">
          <ElSelect v-model="form.status">
            <ElOption
              v-for="option in masterStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="说明" prop="description">
          <ElInput v-model.trim="form.description" maxlength="255" type="textarea" :rows="3" />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="formDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="saveRole">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="permissionDialogVisible" title="配置角色权限" width="760px" destroy-on-close>
      <div class="permission-header">
        <strong>{{ activeRole?.name }}</strong>
        <span>{{ activeRole?.code }}</span>
      </div>
      <ElCheckboxGroup v-model="checkedPermissionIds" class="permission-groups">
        <section v-for="group in permissionTree" :key="group.module" class="permission-group">
          <h3>{{ group.module }}</h3>
          <div class="permission-options">
            <ElCheckbox
              v-for="permission in group.permissions"
              :key="permission.id"
              :label="permission.id"
            >
              {{ permission.name }}
              <span class="permission-code">{{ permission.code }}</span>
            </ElCheckbox>
          </div>
        </section>
      </ElCheckboxGroup>
      <template #footer>
        <ElButton @click="permissionDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="savingPermissions" @click="savePermissions">保存权限</ElButton>
      </template>
    </ElDialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import { permissionApi, roleApi } from '@/api/rbac'
import { masterStatusOptions, statusLabel, statusType } from '@/constants/options'
import { formatDateTime } from '@/utils/format'

const builtInRoleCodes = ['ADMIN', 'MANAGER', 'STAFF', 'VIEWER']

const loading = ref(false)
const saving = ref(false)
const savingPermissions = ref(false)
const formDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const editingId = ref(null)
const activeRole = ref(null)
const formRef = ref()
const records = ref([])
const permissionTree = ref([])
const checkedPermissionIds = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  keyword: '',
  status: ''
})
const form = reactive({
  code: '',
  name: '',
  description: '',
  status: 'ACTIVE'
})
const formRules = {
  code: [
    {
      required: true,
      message: '请输入角色编码',
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      message: '请输入角色名称',
      trigger: 'blur'
    }
  ],
  status: [
    {
      required: true,
      message: '请选择状态',
      trigger: 'change'
    }
  ]
}

function isBuiltIn(row) {
  return builtInRoleCodes.includes(row.code)
}

function resetForm(row = {}) {
  form.code = row.code || ''
  form.name = row.name || ''
  form.description = row.description || ''
  form.status = row.status || 'ACTIVE'
}

async function loadList() {
  loading.value = true
  try {
    const result = await roleApi.page({
      page: pagination.page,
      size: pagination.size,
      keyword: queryForm.keyword,
      status: queryForm.status
    })
    records.value = result.records || []
    pagination.total = result.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  queryForm.keyword = ''
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

async function saveRole() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      code: form.code,
      name: form.name,
      description: form.description,
      status: form.status
    }
    if (editingId.value) {
      await roleApi.update(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await roleApi.create(payload)
      ElMessage.success('新增成功')
    }
    formDialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await roleApi.updateStatus(row.id, {
    status: nextStatus
  })
  ElMessage.success(nextStatus === 'ACTIVE' ? '启用成功' : '停用成功')
  loadList()
}

async function removeRole(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」？`, '删除角色', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await roleApi.remove(row.id)
  ElMessage.success('删除成功')
  loadList()
}

async function openPermissionDialog(row) {
  activeRole.value = row
  permissionDialogVisible.value = true
  const [tree, selected] = await Promise.all([
    permissionApi.tree(),
    roleApi.permissions(row.id)
  ])
  permissionTree.value = tree || []
  checkedPermissionIds.value = (selected || []).map((item) => item.id)
}

async function savePermissions() {
  savingPermissions.value = true
  try {
    await roleApi.updatePermissions(activeRole.value.id, checkedPermissionIds.value)
    ElMessage.success('权限保存成功')
    permissionDialogVisible.value = false
  } finally {
    savingPermissions.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.permission-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.permission-header span,
.permission-code {
  color: var(--wms-muted);
  font-size: 12px;
}

.permission-groups {
  display: grid;
  max-height: 520px;
  gap: 12px;
  overflow: auto;
}

.permission-group {
  border: 1px solid var(--wms-border);
  border-radius: 8px;
  padding: 12px;
}

.permission-group h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.permission-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.permission-code {
  margin-left: 6px;
}

@media (max-width: 768px) {
  .permission-options {
    grid-template-columns: 1fr;
  }
}
</style>
