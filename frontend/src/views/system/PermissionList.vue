<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>权限管理</h1>
        <p>维护菜单、按钮和接口权限点，为角色授权提供基础数据。</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新增权限
      </ElButton>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElInput
          v-model.trim="queryForm.keyword"
          class="filter-keyword"
          clearable
          placeholder="权限编码 / 名称"
          @keyup.enter="loadList"
        />
        <ElInput v-model.trim="queryForm.module" clearable placeholder="模块" @keyup.enter="loadList" />
        <ElSelect v-model="queryForm.type" clearable placeholder="类型">
          <ElOption v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </ElSelect>
        <ElSelect v-model="queryForm.status" clearable placeholder="状态">
          <ElOption
            v-for="option in masterStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
        <ElButton type="primary" @click="loadList">
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
          <ElTableColumn prop="code" label="权限编码" min-width="170" show-overflow-tooltip />
          <ElTableColumn prop="name" label="权限名称" min-width="150" show-overflow-tooltip />
          <ElTableColumn prop="module" label="模块" width="130" />
          <ElTableColumn label="类型" width="100">
            <template #default="{ row }">
              <ElTag effect="plain">{{ typeLabel(row.type) }}</ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="path" label="路径" min-width="190" show-overflow-tooltip />
          <ElTableColumn prop="method" label="方法" width="90" />
          <ElTableColumn label="状态" width="100">
            <template #default="{ row }">
              <ElTag :type="statusType(masterStatusOptions, row.status)" effect="plain">
                {{ statusLabel(masterStatusOptions, row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="160">
            <template #default="{ row }">
              <span class="table-actions">
                <ElButton size="small" type="primary" link @click="openEdit(row)">编辑</ElButton>
                <ElButton size="small" :type="row.status === 'ACTIVE' ? 'warning' : 'success'" link @click="toggleStatus(row)">
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </ElButton>
              </span>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>

    <ElDialog v-model="formDialogVisible" :title="editingId ? '编辑权限' : '新增权限'" width="680px" destroy-on-close>
      <ElForm ref="formRef" :model="form" :rules="formRules" label-width="86px">
        <div class="form-grid">
          <ElFormItem label="权限编码" prop="code">
            <ElInput v-model.trim="form.code" maxlength="100" placeholder="如 product:create" />
          </ElFormItem>
          <ElFormItem label="权限名称" prop="name">
            <ElInput v-model.trim="form.name" maxlength="100" placeholder="请输入权限名称" />
          </ElFormItem>
          <ElFormItem label="类型" prop="type">
            <ElSelect v-model="form.type">
              <ElOption v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="模块" prop="module">
            <ElInput v-model.trim="form.module" maxlength="50" placeholder="如 PRODUCT" />
          </ElFormItem>
          <ElFormItem label="路径" prop="path">
            <ElInput v-model.trim="form.path" maxlength="255" placeholder="/api/products" />
          </ElFormItem>
          <ElFormItem label="方法" prop="method">
            <ElInput v-model.trim="form.method" maxlength="10" placeholder="GET / POST" />
          </ElFormItem>
          <ElFormItem label="排序" prop="sortOrder">
            <ElInputNumber v-model="form.sortOrder" :min="0" :max="9999" />
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
        </div>
      </ElForm>
      <template #footer>
        <ElButton @click="formDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="savePermission">保存</ElButton>
      </template>
    </ElDialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import { permissionApi } from '@/api/rbac'
import { masterStatusOptions, statusLabel, statusType } from '@/constants/options'

const typeOptions = [
  {
    label: '菜单',
    value: 'MENU'
  },
  {
    label: '按钮',
    value: 'BUTTON'
  },
  {
    label: '接口',
    value: 'API'
  }
]

const loading = ref(false)
const saving = ref(false)
const formDialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const records = ref([])
const queryForm = reactive({
  keyword: '',
  module: '',
  type: '',
  status: ''
})
const form = reactive({
  code: '',
  name: '',
  type: 'MENU',
  module: '',
  path: '',
  method: '',
  sortOrder: 0,
  status: 'ACTIVE'
})
const formRules = {
  code: [
    {
      required: true,
      message: '请输入权限编码',
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      message: '请输入权限名称',
      trigger: 'blur'
    }
  ],
  type: [
    {
      required: true,
      message: '请选择类型',
      trigger: 'change'
    }
  ],
  module: [
    {
      required: true,
      message: '请输入模块',
      trigger: 'blur'
    }
  ]
}

function typeLabel(type) {
  return typeOptions.find((option) => option.value === type)?.label || type
}

function resetForm(row = {}) {
  form.code = row.code || ''
  form.name = row.name || ''
  form.type = row.type || 'MENU'
  form.module = row.module || ''
  form.path = row.path || ''
  form.method = row.method || ''
  form.sortOrder = row.sortOrder || 0
  form.status = row.status || 'ACTIVE'
}

async function loadList() {
  loading.value = true
  try {
    records.value = await permissionApi.list(queryForm)
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.module = ''
  queryForm.type = ''
  queryForm.status = ''
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

async function savePermission() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await permissionApi.update(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await permissionApi.create(payload)
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
  await permissionApi.updateStatus(row.id, {
    status: nextStatus
  })
  ElMessage.success(nextStatus === 'ACTIVE' ? '启用成功' : '停用成功')
  loadList()
}

onMounted(loadList)
</script>
