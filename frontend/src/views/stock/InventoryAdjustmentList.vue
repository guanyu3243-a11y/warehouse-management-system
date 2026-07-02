<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>库存调整</h1>
        <p>处理盘盈、盘亏和日常库存修正，确认后同步更新库存并生成库存流水。</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新建调整单
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
          <ElTableColumn prop="adjustmentNo" label="调整单号" min-width="190" show-overflow-tooltip />
          <ElTableColumn prop="warehouseName" label="仓库" min-width="140" show-overflow-tooltip />
          <ElTableColumn prop="reason" label="调整原因" min-width="150" show-overflow-tooltip />
          <ElTableColumn label="调整数量" width="110">
            <template #default="{ row }">
              <span :class="quantityClass(row.totalAdjustQuantity)">
                {{ signedQuantity(row.totalAdjustQuantity) }}
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
          <ElTableColumn prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <ElTableColumn label="更新时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="220">
            <template #default="{ row }">
              <span class="table-actions">
                <ElButton size="small" type="primary" link @click="openDetail(row)">详情</ElButton>
                <ElButton v-if="row.status === 'DRAFT'" size="small" type="primary" link @click="openEdit(row)">
                  编辑
                </ElButton>
                <ElButton
                  v-if="row.status === 'DRAFT'"
                  size="small"
                  type="success"
                  link
                  :loading="confirmingId === row.id"
                  :disabled="cancellingId === row.id"
                  @click="confirmAdjustment(row)"
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
                  @click="cancelAdjustment(row)"
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

    <ElDialog v-model="dialogVisible" :title="editingId ? '编辑库存调整单' : '新建库存调整单'" width="920px">
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
            label="调整原因"
            prop="reason"
            :rules="[{ required: true, message: '请输入调整原因', trigger: 'blur' }]"
          >
            <ElInput v-model.trim="form.reason" maxlength="100" show-word-limit placeholder="例如：盘点差异" />
          </ElFormItem>
          <ElFormItem class="is-wide" label="备注">
            <ElInput v-model.trim="form.remark" maxlength="255" show-word-limit type="textarea" :rows="2" />
          </ElFormItem>
        </div>
      </ElForm>

      <div class="items-toolbar">
        <strong>调整明细</strong>
        <ElButton size="small" type="primary" @click="addItem">
          <ElIcon><Plus /></ElIcon>
          添加明细
        </ElButton>
      </div>

      <ElTable :data="form.items" row-key="clientId" border>
        <ElTableColumn label="型号" min-width="150">
          <template #default="{ row }">
            <ElSelect
              v-model="row.productModel"
              clearable
              filterable
              remote
              reserve-keyword
              :loading="row.productLoading"
              :remote-method="(keyword) => searchRowModels(row, keyword)"
              placeholder="型号"
              @change="handleRowModelChange(row)"
              @visible-change="(visible) => handleRowModelVisibleChange(row, visible)"
            >
              <ElOption
                v-for="option in rowModelOptions(row)"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </ElSelect>
          </template>
        </ElTableColumn>
        <ElTableColumn label="颜色" min-width="130">
          <template #default="{ row }">
            <ElSelect
              v-model="row.productColor"
              clearable
              filterable
              :disabled="!row.productModel"
              placeholder="颜色"
              @change="handleRowColorChange(row)"
              @visible-change="(visible) => handleRowColorVisibleChange(row, visible)"
            >
              <ElOption
                v-for="option in rowColorOptions(row)"
                :key="option"
                :label="option"
                :value="option"
              />
            </ElSelect>
          </template>
        </ElTableColumn>
        <ElTableColumn label="尺码" min-width="130">
          <template #default="{ row }">
            <ElSelect
              v-model="row.productId"
              clearable
              filterable
              :disabled="!row.productModel || !row.productColor"
              placeholder="尺码"
              @change="handleRowProductChange(row)"
            >
              <ElOption
                v-for="product in rowSizeProductOptions(row)"
                :key="product.id"
                :label="product.size || '-'"
                :value="product.id"
              />
            </ElSelect>
          </template>
        </ElTableColumn>
        <ElTableColumn label="调整数量" width="160">
          <template #default="{ row }">
            <ElInputNumber
              v-model="row.adjustQuantity"
              :min="-999999"
              :max="999999"
              controls-position="right"
              style="width: 100%"
            />
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

      <div class="document-total">
        <span>合计调整数量：<strong :class="quantityClass(totalAdjustQuantity)">{{ signedQuantity(totalAdjustQuantity) }}</strong></span>
      </div>

      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="saveAdjustment">保存草稿</ElButton>
      </template>
    </ElDialog>

    <ElDrawer v-model="detailVisible" title="库存调整详情" size="720px">
      <template v-if="currentDocument">
        <div class="drawer-section">
          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="调整单号">{{ currentDocument.adjustmentNo }}</ElDescriptionsItem>
            <ElDescriptionsItem label="状态">
              <ElTag :type="statusType(documentStatusOptions, currentDocument.status)" effect="plain">
                {{ statusLabel(documentStatusOptions, currentDocument.status) }}
              </ElTag>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="仓库">{{ currentDocument.warehouseName }}</ElDescriptionsItem>
            <ElDescriptionsItem label="调整数量">
              <span :class="quantityClass(currentDocument.totalAdjustQuantity)">
                {{ signedQuantity(currentDocument.totalAdjustQuantity) }}
              </span>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="调整原因">{{ currentDocument.reason }}</ElDescriptionsItem>
            <ElDescriptionsItem label="更新时间">{{ formatDateTime(currentDocument.updatedAt) }}</ElDescriptionsItem>
            <ElDescriptionsItem label="备注">{{ currentDocument.remark || '-' }}</ElDescriptionsItem>
          </ElDescriptions>
        </div>

        <div class="drawer-section">
          <h3>调整明细</h3>
          <ElTable :data="currentDocument.items || []" size="small">
            <ElTableColumn prop="sku" label="SKU" min-width="120" show-overflow-tooltip />
            <ElTableColumn prop="productName" label="商品" min-width="160" show-overflow-tooltip />
            <ElTableColumn prop="quantityBefore" label="调整前" width="90" />
            <ElTableColumn label="调整数量" width="100">
              <template #default="{ row }">
                <span :class="quantityClass(row.adjustQuantity)">
                  {{ signedQuantity(row.adjustQuantity) }}
                </span>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="quantityAfter" label="调整后" width="90" />
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

import { inventoryAdjustmentApi, productApi, warehouseApi } from '@/api/business'
import { documentStatusOptions, statusLabel, statusType } from '@/constants/options'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const confirmingId = ref(null)
const cancellingId = ref(null)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const records = ref([])
const warehouses = ref([])
const selectedProducts = ref(new Map())
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
  reason: '',
  remark: '',
  items: []
})

const warehouseOptions = computed(() =>
  warehouses.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)
const totalAdjustQuantity = computed(() =>
  form.items.reduce((sum, item) => sum + Number(item.adjustQuantity || 0), 0)
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

function productModelValue(product = {}) {
  return product.name || String(product.sku || '').split('-')[0] || ''
}

function productsForSelection(sourceProducts, model, color = '') {
  return (sourceProducts || [])
    .filter((product) => !model || productModelValue(product) === model)
    .filter((product) => !color || product.color === color)
    .sort((left, right) => String(left.size || '').localeCompare(String(right.size || ''), 'zh-Hans', { numeric: true }))
}

function modelOptionsFromProducts(sourceProducts) {
  const models = new Map()

  ;(sourceProducts || []).forEach((product) => {
    const model = productModelValue(product)

    if (model && !models.has(model)) {
      models.set(model, {
        label: model,
        value: model
      })
    }
  })

  return [...models.values()]
}

function colorOptionsFromProducts(sourceProducts, model) {
  return [
    ...new Set(
      productsForSelection(sourceProducts, model)
        .map((product) => product.color)
        .filter(Boolean)
    )
  ]
}

function rowModelOptions(row) {
  return modelOptionsFromProducts(row.productCandidates)
}

function rowColorOptions(row) {
  return colorOptionsFromProducts(row.productCandidates, row.productModel)
}

function rowSizeProductOptions(row) {
  return productsForSelection(row.productCandidates, row.productModel, row.productColor)
}

function rememberProduct(product) {
  if (!product?.id) {
    return
  }

  const next = new Map(selectedProducts.value)
  next.set(product.id, product)
  selectedProducts.value = next
}

function rememberProducts(sourceProducts) {
  sourceProducts.forEach(rememberProduct)
}

async function fetchProductCandidates(keyword = '') {
  const result = await productApi.page({
    page: 1,
    size: 100,
    status: 'ACTIVE',
    keyword: keyword?.trim()
  })

  const records = result.records || []
  rememberProducts(records)
  return records
}

async function searchRowModels(row, keyword = '') {
  const requestId = (row.productSearchRequestId || 0) + 1
  row.productSearchRequestId = requestId
  row.productLoading = true

  try {
    const records = await fetchProductCandidates(keyword)

    if (row.productSearchRequestId === requestId) {
      row.productCandidates = records
    }
  } finally {
    if (row.productSearchRequestId === requestId) {
      row.productLoading = false
    }
  }
}

function handleRowModelVisibleChange(row, visible) {
  if (visible && !row.productCandidates.length) {
    searchRowModels(row, row.productModel)
  }
}

function handleRowColorVisibleChange(row, visible) {
  if (visible && row.productModel && !row.productCandidates.length) {
    searchRowModels(row, row.productModel)
  }
}

function handleRowModelChange(row) {
  row.productColor = ''
  row.productId = ''

  if (row.productModel) {
    searchRowModels(row, row.productModel)
  }
}

function handleRowColorChange(row) {
  row.productId = ''
}

function handleRowProductChange(row) {
  const product = row.productCandidates.find((item) => item.id === row.productId)

  if (!product) {
    return
  }

  row.productModel = productModelValue(product)
  row.productColor = product.color || ''
  rememberProduct(product)
}

async function rememberDocumentProducts(items) {
  const productIds = [...new Set(items.map((item) => item.productId).filter(Boolean))]
  const loadedProducts = await Promise.all(
    productIds.map(async (productId) => {
      try {
        return await productApi.detail(productId)
      } catch {
        return null
      }
    })
  )

  loadedProducts.filter(Boolean).forEach(rememberProduct)
}

function createEmptyItem(item = {}) {
  const product = item.productId ? selectedProducts.value.get(item.productId) : null
  const productModel = item.productModel || (product ? productModelValue(product) : item.productName || '')
  const productColor = item.productColor || product?.color || ''

  return {
    clientId: `${Date.now()}-${Math.random()}`,
    productId: item.productId || '',
    productModel,
    productColor,
    productCandidates: product ? [product] : [],
    productLoading: false,
    productSearchRequestId: 0,
    adjustQuantity: item.adjustQuantity ?? 1,
    remark: item.remark || ''
  }
}

async function resetForm(document = {}) {
  form.warehouseId = document.warehouseId || ''
  form.reason = document.reason || ''
  form.remark = document.remark || ''
  await rememberDocumentProducts(document.items || [])
  form.items = document.items?.length ? document.items.map(createEmptyItem) : [createEmptyItem()]
}

async function loadOptions() {
  const warehouseResult = await warehouseApi.page({
    page: 1,
    size: 200,
    status: 'ACTIVE'
  })

  warehouses.value = warehouseResult.records || []
}

async function loadList() {
  loading.value = true

  try {
    const result = await inventoryAdjustmentApi.page({
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
    ElMessage.warning('请添加调整明细')
    return false
  }

  const invalidItem = form.items.find(
    (item) => !item.productId || !item.adjustQuantity || Number(item.adjustQuantity) === 0
  )

  if (invalidItem) {
    ElMessage.warning('请完整填写商品和非 0 调整数量')
    return false
  }

  return true
}

function buildPayload() {
  return {
    warehouseId: form.warehouseId,
    reason: form.reason,
    remark: form.remark,
    items: form.items.map((item) => ({
      productId: item.productId,
      adjustQuantity: item.adjustQuantity,
      remark: item.remark
    }))
  }
}

async function openCreate() {
  editingId.value = null
  selectedProducts.value = new Map()
  await resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  const detail = await inventoryAdjustmentApi.detail(row.id)

  editingId.value = detail.id
  await resetForm(detail)
  dialogVisible.value = true
}

async function openDetail(row) {
  currentDocument.value = await inventoryAdjustmentApi.detail(row.id)
  detailVisible.value = true
}

async function saveAdjustment() {
  await formRef.value?.validate()

  if (!validateItems()) {
    return
  }

  saving.value = true

  try {
    const payload = buildPayload()

    if (editingId.value) {
      await inventoryAdjustmentApi.update(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await inventoryAdjustmentApi.create(payload)
      ElMessage.success('新建成功')
    }

    dialogVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

async function confirmAdjustment(row) {
  if (confirmingId.value || cancellingId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确认库存调整单「${row.adjustmentNo}」？`, '确认调整单', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  confirmingId.value = row.id

  try {
    await inventoryAdjustmentApi.confirm(row.id)
    ElMessage.success('确认成功')
    await loadList()
  } finally {
    confirmingId.value = null
  }
}

async function cancelAdjustment(row) {
  if (confirmingId.value || cancellingId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确认取消库存调整单「${row.adjustmentNo}」？`, '取消调整单', {
      type: 'warning',
      confirmButtonText: '取消调整单',
      cancelButtonText: '返回'
    })
  } catch {
    return
  }

  cancellingId.value = row.id

  try {
    await inventoryAdjustmentApi.cancel(row.id)
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
.items-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 6px 0 12px;
}

.document-total {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  color: var(--wms-ink);
}

.quantity-plus {
  color: #047857;
  font-weight: 700;
}

.quantity-minus {
  color: var(--wms-danger);
  font-weight: 700;
}

@media (max-width: 768px) {
  .document-total {
    justify-content: flex-start;
  }
}
</style>
