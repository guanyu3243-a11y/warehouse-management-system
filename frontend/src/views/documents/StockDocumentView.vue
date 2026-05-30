<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <ElButton type="primary" @click="openCreate">
        <ElIcon><Plus /></ElIcon>
        新建单据
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
        <ElSelect v-if="isStockIn" v-model="queryForm.supplierId" clearable filterable placeholder="供应商">
          <ElOption
            v-for="supplier in supplierOptions"
            :key="supplier.value"
            :label="supplier.label"
            :value="supplier.value"
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
          <ElTableColumn :prop="documentNoField" label="单据编号" min-width="180" show-overflow-tooltip />
          <ElTableColumn prop="warehouseName" label="仓库" min-width="140" show-overflow-tooltip />
          <ElTableColumn
            v-if="isStockIn"
            prop="supplierName"
            label="供应商"
            min-width="160"
            show-overflow-tooltip
          />
          <ElTableColumn prop="totalQuantity" label="总数量" width="100" />
          <ElTableColumn label="总金额" width="120">
            <template #default="{ row }">
              {{ formatMoney(row.totalAmount) }}
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
          <ElTableColumn label="操作" fixed="right" width="210">
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
                  @click="confirmDocument(row)"
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
                  @click="cancelDocument(row)"
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

    <ElDialog v-model="dialogVisible" :title="editingId ? `编辑${title}` : `新建${title}`" width="960px">
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
            v-if="isStockIn"
            label="供应商"
            prop="supplierId"
            :rules="[{ required: true, message: '请选择供应商', trigger: 'change' }]"
          >
            <ElSelect v-model="form.supplierId" filterable placeholder="请选择供应商">
              <ElOption
                v-for="supplier in supplierOptions"
                :key="supplier.value"
                :label="supplier.label"
                :value="supplier.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem class="is-wide" label="备注">
            <ElInput v-model.trim="form.remark" maxlength="255" show-word-limit type="textarea" :rows="2" />
          </ElFormItem>
        </div>
      </ElForm>

      <div class="items-toolbar">
        <strong>单据明细</strong>
        <ElButton size="small" type="primary" @click="addItem">
          <ElIcon><Plus /></ElIcon>
          添加明细
        </ElButton>
      </div>

      <ElTable :data="form.items" row-key="clientId" border>
        <ElTableColumn label="商品" min-width="240">
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
        <ElTableColumn label="数量" width="150">
          <template #default="{ row }">
            <ElInputNumber v-model="row.quantity" :min="1" controls-position="right" style="width: 100%" />
          </template>
        </ElTableColumn>
        <ElTableColumn :label="priceLabel" width="170">
          <template #default="{ row }">
            <ElInputNumber
              v-model="row.price"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              style="width: 100%"
            />
          </template>
        </ElTableColumn>
        <ElTableColumn label="金额" width="120">
          <template #default="{ row }">
            {{ formatMoney(itemAmount(row)) }}
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
        <span>合计数量：{{ totalQuantity }}</span>
        <span>合计金额：{{ formatMoney(totalAmount) }}</span>
      </div>

      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="saveDocument">保存草稿</ElButton>
      </template>
    </ElDialog>

    <ElDrawer v-model="detailVisible" :title="`${title}详情`" size="720px">
      <template v-if="currentDocument">
        <div class="drawer-section">
          <ElDescriptions :column="2" border>
            <ElDescriptionsItem label="单据编号">{{ currentDocument[documentNoField] }}</ElDescriptionsItem>
            <ElDescriptionsItem label="状态">
              <ElTag :type="statusType(documentStatusOptions, currentDocument.status)" effect="plain">
                {{ statusLabel(documentStatusOptions, currentDocument.status) }}
              </ElTag>
            </ElDescriptionsItem>
            <ElDescriptionsItem label="仓库">{{ currentDocument.warehouseName }}</ElDescriptionsItem>
            <ElDescriptionsItem v-if="isStockIn" label="供应商">{{ currentDocument.supplierName }}</ElDescriptionsItem>
            <ElDescriptionsItem label="总数量">{{ currentDocument.totalQuantity }}</ElDescriptionsItem>
            <ElDescriptionsItem label="总金额">{{ formatMoney(currentDocument.totalAmount) }}</ElDescriptionsItem>
            <ElDescriptionsItem label="备注">{{ currentDocument.remark || '-' }}</ElDescriptionsItem>
            <ElDescriptionsItem label="更新时间">{{ formatDateTime(currentDocument.updatedAt) }}</ElDescriptionsItem>
          </ElDescriptions>
        </div>

        <div class="drawer-section">
          <h3>明细</h3>
          <ElTable :data="currentDocument.items || []" size="small">
            <ElTableColumn prop="sku" label="SKU" min-width="120" show-overflow-tooltip />
            <ElTableColumn prop="productName" label="商品" min-width="160" show-overflow-tooltip />
            <ElTableColumn prop="quantity" label="数量" width="90" />
            <ElTableColumn :prop="priceField" :label="priceLabel" width="110">
              <template #default="{ row }">
                {{ formatMoney(row[priceField]) }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="amount" label="金额" width="110">
              <template #default="{ row }">
                {{ formatMoney(row.amount) }}
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

import { categoryApi, productApi, supplierApi, warehouseApi } from '@/api/business'
import { documentStatusOptions, statusLabel, statusType } from '@/constants/options'
import { formatDateTime, formatMoney } from '@/utils/format'

const props = defineProps({
  type: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  api: {
    type: Object,
    required: true
  }
})

const isStockIn = computed(() => props.type === 'in')
const documentNoField = computed(() => (isStockIn.value ? 'stockInNo' : 'stockOutNo'))
const priceField = computed(() => (isStockIn.value ? 'unitCost' : 'unitSalePrice'))
const priceLabel = computed(() => (isStockIn.value ? '入库单价' : '出库单价'))
const loading = ref(false)
const saving = ref(false)
const confirmingId = ref(null)
const cancellingId = ref(null)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const records = ref([])
const categories = ref([])
const warehouses = ref([])
const suppliers = ref([])
const products = ref([])
const currentDocument = ref(null)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  status: '',
  warehouseId: '',
  supplierId: ''
})
const form = reactive({
  warehouseId: '',
  supplierId: '',
  remark: '',
  items: []
})

const categoryNameMap = computed(() => new Map(categories.value.map((item) => [item.id, item.name])))
const warehouseOptions = computed(() =>
  warehouses.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)
const supplierOptions = computed(() =>
  suppliers.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)
const productOptions = computed(() =>
  products.value.map((item) => ({
    label: `${item.sku} - ${item.name}${categoryNameMap.value.get(item.categoryId) ? ` / ${categoryNameMap.value.get(item.categoryId)}` : ''}`,
    value: item.id
  }))
)
const totalQuantity = computed(() => form.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const totalAmount = computed(() => form.items.reduce((sum, item) => sum + itemAmount(item), 0))

function itemAmount(item) {
  return Number(item.quantity || 0) * Number(item.price || 0)
}

function createEmptyItem(item = {}) {
  return {
    clientId: `${Date.now()}-${Math.random()}`,
    productId: item.productId || '',
    quantity: item.quantity || 1,
    price: item[priceField.value] || 0,
    remark: item.remark || ''
  }
}

function resetForm(document = {}) {
  form.warehouseId = document.warehouseId || ''
  form.supplierId = document.supplierId || ''
  form.remark = document.remark || ''
  form.items = document.items?.length ? document.items.map(createEmptyItem) : [createEmptyItem()]
}

async function loadOptions() {
  const [categoryResult, warehouseResult, supplierResult, productResult] = await Promise.all([
    categoryApi.page({
      page: 1,
      size: 200,
      status: 'ACTIVE'
    }),
    warehouseApi.page({
      page: 1,
      size: 200,
      status: 'ACTIVE'
    }),
    supplierApi.page({
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

  categories.value = categoryResult.records || []
  warehouses.value = warehouseResult.records || []
  suppliers.value = supplierResult.records || []
  products.value = productResult.records || []
}

async function loadList() {
  loading.value = true

  try {
    const result = await props.api.page({
      page: pagination.page,
      size: pagination.size,
      status: queryForm.status,
      warehouseId: queryForm.warehouseId,
      supplierId: isStockIn.value ? queryForm.supplierId : undefined
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
  queryForm.supplierId = ''
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
    ElMessage.warning('请添加单据明细')
    return false
  }

  const invalidItem = form.items.find((item) => !item.productId || !item.quantity || Number(item.quantity) <= 0)

  if (invalidItem) {
    ElMessage.warning('请完整填写商品和数量')
    return false
  }

  return true
}

function buildPayload() {
  const payload = {
    warehouseId: form.warehouseId,
    remark: form.remark,
    items: form.items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
      remark: item.remark,
      [priceField.value]: item.price
    }))
  }

  if (isStockIn.value) {
    payload.supplierId = form.supplierId
  }

  return payload
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  const detail = await props.api.detail(row.id)

  editingId.value = detail.id
  resetForm(detail)
  dialogVisible.value = true
}

async function openDetail(row) {
  currentDocument.value = await props.api.detail(row.id)
  detailVisible.value = true
}

async function saveDocument() {
  await formRef.value?.validate()

  if (!validateItems()) {
    return
  }

  saving.value = true

  try {
    const payload = buildPayload()

    if (editingId.value) {
      await props.api.update(editingId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await props.api.create(payload)
      ElMessage.success('新建成功')
    }

    dialogVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

async function confirmDocument(row) {
  if (confirmingId.value || cancellingId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认${isStockIn.value ? '入库' : '出库'}单据「${row[documentNoField.value]}」？`,
      '确认单据',
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  confirmingId.value = row.id

  try {
    await props.api.confirm(row.id)
    ElMessage.success('确认成功')
    await loadList()
  } finally {
    confirmingId.value = null
  }
}

async function cancelDocument(row) {
  if (confirmingId.value || cancellingId.value) {
    return
  }

  try {
    await ElMessageBox.confirm(`确认取消单据「${row[documentNoField.value]}」？`, '取消单据', {
      type: 'warning',
      confirmButtonText: '取消单据',
      cancelButtonText: '返回'
    })
  } catch {
    return
  }

  cancellingId.value = row.id

  try {
    await props.api.cancel(row.id)
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
  gap: 20px;
  padding-top: 12px;
  color: var(--wms-ink);
  font-weight: 650;
}

@media (max-width: 768px) {
  .document-total {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
