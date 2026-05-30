<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>库存流水</h1>
        <p>追踪每一次库存变化的来源、变更前数量、变更数量和变更后数量。</p>
      </div>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElSelect v-model="queryForm.productId" clearable filterable placeholder="商品">
          <ElOption
            v-for="product in productOptions"
            :key="product.value"
            :label="product.label"
            :value="product.value"
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
        <ElSelect v-model="queryForm.movementType" clearable placeholder="流水类型">
          <ElOption
            v-for="option in stockMovementTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
        <ElSelect v-model="queryForm.sourceType" clearable placeholder="来源类型">
          <ElOption label="入库单" value="STOCK_IN" />
          <ElOption label="出库单" value="STOCK_OUT" />
        </ElSelect>
        <ElInput
          v-model.trim="queryForm.sourceNo"
          class="filter-keyword"
          clearable
          placeholder="来源单号"
          @keyup.enter="loadList"
        />
        <ElDatePicker
          v-model="queryForm.timeRange"
          end-placeholder="结束时间"
          range-separator="至"
          start-placeholder="开始时间"
          type="datetimerange"
        />
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
          <ElTableColumn prop="movementNo" label="流水号" min-width="190" show-overflow-tooltip />
          <ElTableColumn label="类型" width="110">
            <template #default="{ row }">
              <ElTag :type="movementTypeMeta(row.movementType).type" effect="plain">
                {{ movementTypeMeta(row.movementType).label }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="sourceNo" label="来源单号" min-width="180" show-overflow-tooltip />
          <ElTableColumn prop="productSku" label="SKU" min-width="130" show-overflow-tooltip />
          <ElTableColumn prop="productName" label="商品名称" min-width="170" show-overflow-tooltip />
          <ElTableColumn prop="warehouseName" label="仓库" min-width="140" show-overflow-tooltip />
          <ElTableColumn prop="quantityBefore" label="变更前" width="90" />
          <ElTableColumn label="变更数量" width="100">
            <template #default="{ row }">
              <span :class="row.changeQuantity >= 0 ? 'quantity-plus' : 'quantity-minus'">
                {{ row.changeQuantity >= 0 ? `+${row.changeQuantity}` : row.changeQuantity }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="quantityAfter" label="变更后" width="90" />
          <ElTableColumn prop="operatorUsername" label="操作人" width="120" />
          <ElTableColumn prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <ElTableColumn label="时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
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
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'

import { productApi, stockMovementApi, warehouseApi } from '@/api/business'
import { stockMovementTypeOptions } from '@/constants/options'
import { formatDateTime, formatDateTimeParam } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const products = ref([])
const warehouses = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  productId: '',
  warehouseId: '',
  movementType: '',
  sourceType: '',
  sourceNo: '',
  timeRange: []
})

const productOptions = computed(() =>
  products.value.map((item) => ({
    label: `${item.sku} - ${item.name}`,
    value: item.id
  }))
)
const warehouseOptions = computed(() =>
  warehouses.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)

function movementTypeMeta(value) {
  return stockMovementTypeOptions.find((option) => option.value === value) || {
    label: value || '-',
    type: 'info'
  }
}

async function loadOptions() {
  const [productResult, warehouseResult] = await Promise.all([
    productApi.page({
      page: 1,
      size: 200,
      status: 'ACTIVE'
    }),
    warehouseApi.page({
      page: 1,
      size: 200,
      status: 'ACTIVE'
    })
  ])

  products.value = productResult.records || []
  warehouses.value = warehouseResult.records || []
}

async function loadList() {
  loading.value = true
  try {
    const [startTime, endTime] = queryForm.timeRange || []
    const result = await stockMovementApi.page({
      page: pagination.page,
      size: pagination.size,
      productId: queryForm.productId,
      warehouseId: queryForm.warehouseId,
      movementType: queryForm.movementType,
      sourceType: queryForm.sourceType,
      sourceNo: queryForm.sourceNo,
      startTime: formatDateTimeParam(startTime),
      endTime: formatDateTimeParam(endTime)
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
  queryForm.productId = ''
  queryForm.warehouseId = ''
  queryForm.movementType = ''
  queryForm.sourceType = ''
  queryForm.sourceNo = ''
  queryForm.timeRange = []
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

onMounted(async () => {
  await loadOptions()
  await loadList()
})
</script>

<style scoped>
.quantity-plus {
  color: #047857;
  font-weight: 700;
}

.quantity-minus {
  color: var(--wms-danger);
  font-weight: 700;
}
</style>
