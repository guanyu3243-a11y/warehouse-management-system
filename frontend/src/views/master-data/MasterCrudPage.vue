<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新增
      </ElButton>
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
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import { statusLabel, statusType } from '@/constants/options'

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
const records = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
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

onMounted(() => {
  initQueryForm()
  loadList()
})
</script>
