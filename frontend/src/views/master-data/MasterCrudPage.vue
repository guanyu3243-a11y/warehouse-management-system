<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <div class="page-actions">
        <ElButton v-if="canDownloadTemplate" :loading="downloadingTemplate" @click="handleDownloadTemplate">
          <ElIcon><Download /></ElIcon>
          下载模板
        </ElButton>
        <ElButton v-if="canImport" @click="openImport">
          <ElIcon><Upload /></ElIcon>
          导入
        </ElButton>
        <ElButton v-if="canExport" :loading="exporting" @click="handleExport">
          <ElIcon><Download /></ElIcon>
          导出
        </ElButton>
        <ElButton type="primary" @click="openCreate">
          <ElIcon><Plus /></ElIcon>
          新增
        </ElButton>
      </div>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <template v-for="filter in filters" :key="filter.prop">
          <ElInput
            v-if="filter.type === 'input'"
            v-model.trim="queryForm[filter.prop]"
            :class="{ 'filter-keyword': filter.keyword }"
            clearable
            :placeholder="filter.placeholder || filter.label"
            @keyup.enter="loadList"
          />
          <ElSelect
            v-else-if="filter.type === 'select'"
            v-model="queryForm[filter.prop]"
            clearable
            :placeholder="filter.placeholder || filter.label"
          >
            <ElOption
              v-for="option in filter.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </ElSelect>
        </template>

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
          <ElTableColumn
            v-for="column in columns"
            :key="column.prop"
            :prop="column.prop"
            :label="column.label"
            :min-width="column.minWidth"
            :width="column.width"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <ElTag
                v-if="column.statusOptions"
                :type="statusType(column.statusOptions, row[column.prop])"
                effect="plain"
              >
                {{ statusLabel(column.statusOptions, row[column.prop]) }}
              </ElTag>
              <span v-else-if="column.formatter">{{ column.formatter(row[column.prop], row) }}</span>
              <span v-else>{{ row[column.prop] ?? '-' }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="150">
            <template #default="{ row }">
              <span class="table-actions">
                <ElButton size="small" type="primary" link @click="openEdit(row)">编辑</ElButton>
                <ElButton size="small" type="danger" link @click="handleDelete(row)">删除</ElButton>
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

    <ElDialog
      v-model="dialogVisible"
      :title="editingId ? `编辑${title}` : `新增${title}`"
      width="680px"
      destroy-on-close
    >
      <ElForm ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <div class="form-grid">
          <ElFormItem
            v-for="field in fields"
            :key="field.prop"
            :class="{ 'is-wide': field.wide }"
            :label="field.label"
            :prop="field.prop"
          >
            <ElInput
              v-if="field.type === 'textarea'"
              v-model.trim="form[field.prop]"
              :maxlength="field.maxlength"
              show-word-limit
              type="textarea"
              :rows="field.rows || 3"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />
            <ElSelect
              v-else-if="field.type === 'select'"
              v-model="form[field.prop]"
              :placeholder="field.placeholder || `请选择${field.label}`"
              clearable
              filterable
            >
              <ElOption
                v-for="option in field.options || []"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </ElSelect>
            <ElInputNumber
              v-else-if="field.type === 'number'"
              v-model="form[field.prop]"
              :min="field.min ?? 0"
              :precision="field.precision"
              :step="field.step || 1"
              controls-position="right"
              style="width: 100%"
            />
            <ElInput
              v-else
              v-model.trim="form[field.prop]"
              :maxlength="field.maxlength"
              :placeholder="field.placeholder || `请输入${field.label}`"
            />
          </ElFormItem>
        </div>
      </ElForm>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="handleSave">保存</ElButton>
      </template>
    </ElDialog>

    <ElDialog v-model="importDialogVisible" title="导入数据" width="560px" destroy-on-close>
      <ElUpload
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <ElIcon class="el-icon--upload"><Upload /></ElIcon>
        <div class="el-upload__text">拖拽 Excel 文件到此处，或点击选择</div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx 文件，请先下载模板并按表头填写。</div>
        </template>
      </ElUpload>

      <div v-if="importResult" class="import-result">
        <ElAlert
          :title="`导入完成：成功 ${importResult.successCount || 0} 行，失败 ${importResult.failCount || 0} 行`"
          :type="importResult.failCount ? 'warning' : 'success'"
          show-icon
          :closable="false"
        />
        <ElTable
          v-if="importResult.failures?.length"
          :data="importResult.failures"
          size="small"
          max-height="220"
        >
          <ElTableColumn prop="rowNumber" label="行号" width="90" />
          <ElTableColumn prop="reason" label="失败原因" min-width="260" show-overflow-tooltip />
        </ElTable>
      </div>

      <template #footer>
        <ElButton @click="importDialogVisible = false">关闭</ElButton>
        <ElButton type="primary" :loading="importing" :disabled="!importFile" @click="handleImport">开始导入</ElButton>
      </template>
    </ElDialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'

import { statusLabel, statusType } from '@/constants/options'
import { downloadBlob } from '@/utils/download'

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  api: {
    type: Object,
    required: true
  },
  filters: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    required: true
  },
  fields: {
    type: Array,
    required: true
  },
  defaultForm: {
    type: Object,
    default: () => ({})
  }
})

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const downloadingTemplate = ref(false)
const importing = ref(false)
const records = ref([])
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const importFile = ref(null)
const importResult = ref(null)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({})
const form = reactive({})

const formRules = computed(() =>
  Object.fromEntries(
    props.fields
      .filter((field) => field.required)
      .map((field) => [
        field.prop,
        [
          {
            required: true,
            message: field.type === 'select' ? `请选择${field.label}` : `请输入${field.label}`,
            trigger: field.type === 'select' ? 'change' : 'blur'
          }
        ]
      ])
  )
)
const canExport = computed(() => typeof props.api.export === 'function')
const canImport = computed(() => typeof props.api.importFile === 'function')
const canDownloadTemplate = computed(() => typeof props.api.importTemplate === 'function')

function initQueryForm() {
  for (const filter of props.filters) {
    queryForm[filter.prop] = filter.defaultValue ?? ''
  }
}

function initForm(row = {}) {
  for (const field of props.fields) {
    form[field.prop] = row[field.prop] ?? props.defaultForm[field.prop] ?? field.defaultValue ?? ''
  }
}

function buildPayload() {
  return Object.fromEntries(props.fields.map((field) => [field.prop, form[field.prop]]))
}

async function loadList() {
  loading.value = true

  try {
    const result = await props.api.page({
      page: pagination.page,
      size: pagination.size,
      ...queryForm
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
  initQueryForm()
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

function openCreate() {
  editingId.value = null
  initForm()
  dialogVisible.value = true
}

function openImport() {
  importFile.value = null
  importResult.value = null
  importDialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  initForm(row)
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true

  try {
    const payload = buildPayload()

    if (editingId.value) {
      await props.api.update(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await props.api.create(payload)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.name || row.code || row.sku}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await props.api.remove(row.id)
  ElMessage.success('删除成功')
  loadList()
}

async function handleExport() {
  exporting.value = true

  try {
    const blob = await props.api.export(queryForm)
    downloadBlob(blob, `${props.title}.xlsx`)
  } finally {
    exporting.value = false
  }
}

async function handleDownloadTemplate() {
  downloadingTemplate.value = true

  try {
    const blob = await props.api.importTemplate()
    downloadBlob(blob, `${props.title}-导入模板.xlsx`)
  } finally {
    downloadingTemplate.value = false
  }
}

function handleFileChange(uploadFile) {
  importFile.value = uploadFile.raw || null
  importResult.value = null
}

function handleFileRemove() {
  importFile.value = null
}

async function handleImport() {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的 Excel 文件')
    return
  }

  importing.value = true

  try {
    importResult.value = await props.api.importFile(importFile.value)
    if (!importResult.value?.failCount) {
      ElMessage.success('导入成功')
      importDialogVisible.value = false
    } else if (importResult.value.successCount) {
      ElMessage.warning('部分数据导入失败')
    } else {
      ElMessage.error('导入失败')
    }
    await loadList()
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  initQueryForm()
  loadList()
})
</script>

<style scoped>
.page-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.import-result {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}
</style>
