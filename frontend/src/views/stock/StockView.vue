<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>{{ isLowStockMode ? '低库存预警' : '库存查询' }}</h1>
        <p>{{ isLowStockMode ? '查看已低于商品预警阈值的库存项。' : '按仓库、分类、SKU 或商品名称查询当前库存。' }}</p>
      </div>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElInput
          v-model.trim="queryForm.keyword"
          class="filter-keyword"
          clearable
          placeholder="SKU / 商品名称"
          @keyup.enter="loadList"
        />
        <ElSelect v-model="queryForm.warehouseId" clearable filterable placeholder="仓库">
          <ElOption
            v-for="warehouse in warehouseOptions"
            :key="warehouse.value"
            :label="warehouse.label"
            :value="warehouse.value"
          />
        </ElSelect>
        <ElSelect v-model="queryForm.categoryId" clearable filterable placeholder="分类">
          <ElOption
            v-for="category in categoryOptions"
            :key="category.value"
            :label="category.label"
            :value="category.value"
          />
        </ElSelect>
        <ElSelect v-if="!isLowStockMode" v-model="queryForm.lowStockOnly" clearable placeholder="库存状态">
          <ElOption label="全部库存" :value="false" />
          <ElOption label="仅低库存" :value="true" />
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
          <ElTableColumn prop="sku" label="SKU" min-width="130" show-overflow-tooltip />
          <ElTableColumn prop="productName" label="商品名称" min-width="180" show-overflow-tooltip />
          <ElTableColumn prop="warehouseName" label="仓库" min-width="150" show-overflow-tooltip />
          <ElTableColumn prop="quantity" label="库存数量" width="100" />
          <ElTableColumn prop="lockedQuantity" label="锁定数量" width="100" />
          <ElTableColumn prop="availableQuantity" label="可用数量" width="100" />
          <ElTableColumn prop="lowStockThreshold" label="预警阈值" width="100" />
          <ElTableColumn label="状态" width="110">
            <template #default="{ row }">
              <ElTag :type="row.lowStock ? 'danger' : 'success'" effect="plain">
                {{ row.lowStock ? '低库存' : '正常' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="更新时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'

import { categoryApi, stockApi, warehouseApi } from '@/api/business'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const categories = ref([])
const warehouses = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  keyword: '',
  warehouseId: '',
  categoryId: '',
  lowStockOnly: ''
})

const isLowStockMode = computed(() => route.name === 'low-stock')
const categoryOptions = computed(() =>
  categories.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)
const warehouseOptions = computed(() =>
  warehouses.value.map((item) => ({
    label: `${item.code} - ${item.name}`,
    value: item.id
  }))
)

async function loadOptions() {
  const [categoryResult, warehouseResult] = await Promise.all([
    categoryApi.page({
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

  categories.value = categoryResult.records || []
  warehouses.value = warehouseResult.records || []
}

async function loadList() {
  loading.value = true

  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: queryForm.keyword,
      warehouseId: queryForm.warehouseId,
      categoryId: queryForm.categoryId,
      lowStockOnly: queryForm.lowStockOnly
    }
    const result = isLowStockMode.value ? await stockApi.lowStock(params) : await stockApi.page(params)

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
  queryForm.warehouseId = ''
  queryForm.categoryId = ''
  queryForm.lowStockOnly = ''
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

watch(
  () => route.name,
  () => {
    handleReset()
  }
)

onMounted(async () => {
  await loadOptions()
  await loadList()
})
</script>
