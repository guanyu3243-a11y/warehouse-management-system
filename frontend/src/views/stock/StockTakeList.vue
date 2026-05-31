<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>库存盘点</h1>
        <p>录入账面数量和实盘数量，确认后按差异更新库存并生成库存流水。</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新建盘点单
      </ElButton>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElSelect v-model="queryForm.status" clearable placeholder="单据状态">
          <ElOption
            v-for="option in documentStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
        <ElSelect v-model="queryForm.warehouseId" clearable filterable placeholder="仓库">
          <ElOption
            v-for="warehouse in warehouseOptions"
            :key="warehouse.value"
            :label="warehouse.label"
            :value="warehouse.value"
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
          <ElTableColumn prop="stockTakeNo" label="盘点单号" min-width="190" show-overflow-tooltip />
          <ElTableColumn prop="title" label="盘点主题" min-width="150" show-overflow-tooltip />
          <ElTableColumn prop="warehouseName" label="仓库" min-width="140" show-overflow-tooltip />
          <ElTableColumn prop="totalBookQuantity" label="账面数" width="90" />
          <ElTableColumn prop="totalActualQuantity" label="实盘数" width="90" />
          <ElTableColumn label="差异数" width="90">
            <template #default="{ row }">
              <span :class="quantityClass(row.totalDifferenceQuantity)">
                {{ signedQuantity(row.totalDifferenceQuantity) }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <ElTag :type="statusType(documentStatusOptions, row.status)" effect="plain">
                {{ statusLabel(documentStatusOptions, row.status) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="更新时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="300">
            <template #default="{ row }">
              <span class="table-actions">
                <ElButton size="small" type="primary" link @click="openDetail(row)">详情</ElButton>
                <ElButton v-if="row.status === 'DRAFT'" size="small" type="primary" link @click="openEdit(row)">
                  编辑
                </ElButton>
                <ElButton
                  v-if="row.status === 'DRAFT'"
                  size="small"
                  type="primary"
                  link
                  :loading="uploadingId === row.id"
                  @click="openImport(row)"
                >
                  导入
                </ElButton>
                <ElButton size="small" type="primary" link :loading="exportingId === row.id" @click="exportItems(row)">
                  导出
                </ElButton>
                <ElButton
                  v-if="row.status === 'DRAFT'"
                  size="small"
                  type="success"
                  link
                  :loading="confirmingId === row.id"
                  :disabled="cancellingId === row.id"
                  @click="confirmStockTake(row)"
                >
                  确认
                </ElButton>
                <ElButton
                  v-if="row.status === 'DRAFT'"
                  size="small"
                  type="warning"
                  link
                  :loading="cancellingId === row.id"
                  :disabled="confirmingId === row.id"
                  @click="cancelStockTake(row)"
                >
                  取消
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

    <input ref="fileInputRef" class="hidden-file" type="file" accept=".xlsx" @change="handleImportFile" />

    <ElDialog v-model="dialogVisible" :title="editingId ? '编辑库存盘点单' : '新建库存盘点单'" width="960px">
      <ElForm ref="formRef" :model="form" label-width="92px">
        <div class="form-grid">
          <ElFormItem
            label="仓库"
            prop="warehouseId"
            :rules="[{ required: true, message: '请选择仓库', trigger: 'change' }]"
          >
            <ElSelect v-model="form.warehouseId" filterable placeholder="请选择仓库">
              <ElOption
                v-for="warehouse in warehouseOptions"
                :key="warehouse.value"
                :label="warehouse.label"
                :value="warehouse.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem
            label="盘点主题"
            prop="title"
            :rules="[{ required: true, message: '请输入盘点主题', trigger: 'blur' }]"
          >
            <ElInput v-model.trim="form.title" maxlength="100" show-word-limit placeholder="例如：月末盘点" />
          </ElFormItem>
          <ElFormItem class="is-wide" label="备注">
            <ElInput v-model.trim="form.remark" maxlength="255" show-word-limit type="textarea" :rows="2" />
          </ElFormItem>
        </div>
      </ElForm>

      <div class="items-toolbar">
        <strong>盘点明细</strong>
        <ElButton size="small" type="primary" @click="addItem">
          <ElIcon><Plus /></ElIcon>
          添加明细
        </ElButton>
      </div>

      <ElTable :data="form.items" row-key="clientId" border>
        <ElTableColumn label="商品" min-width="260">
          <template #default="{ row }">
            <ElSelect v-model="row.productId" filterable placeholder="请选择商品">
              <ElOption
                v-for="product in productOptions"
                :key="product.value"
                :label="product.label"
                :value="product.value"
              />
            </ElSelect>
          </template>
        </ElTableColumn>
        <ElTableColumn label="实盘数量" width="150">
          <template #default="{ row }">
            <ElInputNumber v-model="row.actualQuantity" :min="0" controls-position="right" style="width: 100%" />
          </template>
        </ElTableColumn>
        <ElTableColumn label="备注" min-width="180">
          <template #default="{ row }">
            <ElInput v-model.trim="row.remark" maxlength="255" placeholder="备注" />
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="80">
          <template #default="{ $index }">
            <ElButton link type="danger" size="small" @click="removeItem($index)">移除</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="saveStockTake">保存草稿</ElButton>
      </template>
    </ElDialog>

    <ElDrawer v-model="detailVisible" title="库存盘点详情" size="780px">
      <template v-if="currentDocument">
        <div class="drawer-section">
          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="盘点单号">{{ currentDocument.stockTakeNo }}</ElDescriptionsItem>
            <ElDescriptionsItem label="状态">
              <ElTag :type="statusType(documentStatusOptions, currentDocument.status)" effect="plain">
                {{ statusLabel(documentStatusOptions, currentDocument.status) }}
              </ElTag>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="盘点主题">{{ currentDocument.title }}</ElDescriptionsItem>
            <ElDescriptionsItem label="仓库">{{ currentDocument.warehouseName }}</ElDescriptionsItem>
            <ElDescriptionsItem label="账面数">{{ currentDocument.totalBookQuantity }}</ElDescriptionsItem>
            <ElDescriptionsItem label="实盘数">{{ currentDocument.totalActualQuantity }}</ElDescriptionsItem>
            <ElDescriptionsItem label="差异数">
              <span :class="quantityClass(currentDocument.totalDifferenceQuantity)">
                {{ signedQuantity(currentDocument.totalDifferenceQuantity) }}
              </span>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="更新时间">{{ formatDateTime(currentDocument.updatedAt) }}</ElDescriptionsItem>
            <ElDescriptionsItem label="备注">{{ currentDocument.remark || '-' }}</ElDescriptionsItem>
          </ElDescriptions>
        </div>

        <div class="drawer-section">
          <h3>盘点明细</h3>
          <ElTable :data="currentDocument.items || []" size="small">
            <ElTableColumn prop="sku" label="SKU" min-width="120" show-overflow-tooltip />
            <ElTableColumn prop="productName" label="商品" min-width="160" show-overflow-tooltip />
            <ElTableColumn prop="bookQuantity" label="账面数" width="90" />
            <ElTableColumn prop="actualQuantity" label="实盘数" width="90" />
            <ElTableColumn label="差异数" width="90">
              <template #default="{ row }">
                <span :class="quantityClass(row.differenceQuantity)">
                  {{ signedQuantity(row.differenceQuantity) }}
                </span>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          </ElTable>
        </div>
      </template>
    </ElDrawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import { productApi, stockTakeApi, warehouseApi } from '@/api/business'
import { documentStatusOptions, statusLabel, statusType } from '@/constants/options'
import { downloadBlob } from '@/utils/download'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const uploadingId = ref(null)
const exportingId = ref(null)
const confirmingId = ref(null)
const cancellingId = ref(null)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const importTargetId = ref(null)
const formRef = ref()
const fileInputRef = ref()
const records = ref([])
const products = ref([])
const warehouses = ref([])
const currentDocument = ref(null)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  status: '',
  warehouseId: ''
})
const form = reactive({
  warehouseId: '',
  title: '',
  remark: '',
  items: []
})

const warehouseOptions = computed(() =>
  warehouses.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)
const productOptions = computed(() =>
  products.value.map((item) => ({
    label: `${item.sku} - ${item.name}`,
    value: item.id
  }))
)

function signedQuantity(value) {
  const number = Number(value || 0)
  return number > 0 ? `+${number}` : `${number}`
}

function quantityClass(value) {
  const number = Number(value || 0)
  if (number > 0) {
    return 'quantity-plus'
  }
  if (number < 0) {
    return 'quantity-minus'
  }
  return 'text-muted'
}

function createEmptyItem(item = {}) {
  return {
    clientId: `${Date.now()}-${Math.random()}`,
    productId: item.productId || '',
    actualQuantity: item.actualQuantity ?? 0,
    remark: item.remark || ''
  }
}

function resetForm(document = {}) {
  form.warehouseId = document.warehouseId || ''
  form.title = document.title || ''
  form.remark = document.remark || ''
  form.items = document.items?.length ? document.items.map(createEmptyItem) : [createEmptyItem()]
}

async function loadOptions() {
  const [warehouseResult, productResult] = await Promise.all([
    warehouseApi.page({
      page: 1,
      size: 200,
      status: 'ACTIVE'
    }),
    productApi.page({
      page: 1,
      size: 500,
      status: 'ACTIVE'
    })
  ])

  warehouses.value = warehouseResult.records || []
  products.value = productResult.records || []
}

async function loadList() {
  loading.value = true

  try {
    const result = await stockTakeApi.page({
      page: pagination.page,
      size: pagination.size,
      status: queryForm.status,
      warehouseId: queryForm.warehouseId
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
  queryForm.status = ''
  queryForm.warehouseId = ''
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

function addItem() {
  form.items.push(createEmptyItem())
}

function removeItem(index) {
  if (form.items.length === 1) {
    ElMessage.warning('至少保留一条明细')
    return
  }

  form.items.splice(index, 1)
}

function validateItems() {
  if (!form.items.length) {
    ElMessage.warning('请添加盘点明细')
    return false
  }

  const productIds = new Set()
  for (const item of form.items) {
    if (!item.productId || item.actualQuantity === null || item.actualQuantity === undefined || item.actualQuantity < 0) {
      ElMessage.warning('请完整填写商品和实盘数量')
      return false
    }
    if (productIds.has(item.productId)) {
      ElMessage.warning('同一商品不能重复盘点')
      return false
    }
    productIds.add(item.productId)
  }

  return true
}

function buildPayload() {
  return {
    warehouseId: form.warehouseId,
    title: form.title,
    remark: form.remark,
    items: form.items.map((item) => ({
      productId: item.productId,
      actualQuantity: item.actualQuantity,
      remark: item.remark
    }))
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  const detail = await stockTakeApi.detail(row.id)

  editingId.value = detail.id
  resetForm(detail)
  dialogVisible.value = true
}

async function openDetail(row) {
  currentDocument.value = await stockTakeApi.detail(row.id)
  detailVisible.value = true
}

async function saveStockTake() {
  await formRef.value?.validate()

  if (!validateItems()) {
    return
  }

  saving.value = true

  try {
    const payload = buildPayload()

    if (editingId.value) {
      await stockTakeApi.update(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await stockTakeApi.create(payload)
      ElMessage.success('新建成功')
    }

    dialogVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

function openImport(row) {
  importTargetId.value = row.id
  fileInputRef.value.value = ''
  fileInputRef.value.click()
}

async function handleImportFile(event) {
  const file = event.target.files?.[0]
  if (!file || !importTargetId.value) {
    return
  }

  uploadingId.value = importTargetId.value

  try {
    const result = await stockTakeApi.importFile(importTargetId.value, file)
    if (result.failCount > 0) {
      ElMessage.warning(`导入失败 ${result.failCount} 行，请检查文件`)
    } else {
      ElMessage.success(`导入成功 ${result.successCount} 行`)
      await loadList()
    }
  } finally {
    uploadingId.value = null
    importTargetId.value = null
    event.target.value = ''
  }
}

async function exportItems(row) {
  exportingId.value = row.id

  try {
    const blob = await stockTakeApi.exportItems(row.id)
    downloadBlob(blob, `${row.stockTakeNo || 'stock-take'}.xlsx`)
  } finally {
    exportingId.value = null
  }
}

async function confirmStockTake(row) {
  if (confirmingId.value || cancellingId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确认库存盘点单「${row.stockTakeNo}」？`, '确认盘点单', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  confirmingId.value = row.id

  try {
    await stockTakeApi.confirm(row.id)
    ElMessage.success('确认成功')
    await loadList()
  } finally {
    confirmingId.value = null
  }
}

async function cancelStockTake(row) {
  if (confirmingId.value || cancellingId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确认取消库存盘点单「${row.stockTakeNo}」？`, '取消盘点单', {
      type: 'warning',
      confirmButtonText: '取消盘点单',
      cancelButtonText: '返回'
    })
  } catch {
    return
  }

  cancellingId.value = row.id

  try {
    await stockTakeApi.cancel(row.id)
    ElMessage.success('已取消')
    await loadList()
  } finally {
    cancellingId.value = null
  }
}

onMounted(async () => {
  await loadOptions()
  await loadList()
})
</script>

<style scoped>
.hidden-file {
  display: none;
}

.items-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 6px 0 12px;
}

.quantity-plus {
  color: #047857;
  font-weight: 700;
}

.quantity-minus {
  color: var(--wms-danger);
  font-weight: 700;
}
</style>
