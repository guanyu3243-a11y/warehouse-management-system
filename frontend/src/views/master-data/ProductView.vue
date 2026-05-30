<template>
  <MasterCrudPage
    title="服装商品"
    description="维护 SKU、分类、尺码、颜色、季节、价格和低库存阈值。"
    :api="productApi"
    :filters="filters"
    :columns="columns"
    :fields="fields"
    :default-form="{ status: 'ACTIVE', lowStockThreshold: 0, costPrice: 0, salePrice: 0 }"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

import { categoryApi, productApi } from '@/api/business'
import { masterStatusOptions } from '@/constants/options'
import { formatDateTime, formatMoney } from '@/utils/format'
import MasterCrudPage from './MasterCrudPage.vue'

const categories = ref([])

const categoryOptions = computed(() =>
  categories.value.map((category) => ({
    label: `${category.code} - ${category.name}`,
    value: category.id
  }))
)

function categoryLabel(categoryId) {
  const category = categories.value.find((item) => item.id === categoryId)

  return category ? category.name : categoryId || '-'
}

const filters = computed(() => [
  {
    prop: 'keyword',
    label: '关键词',
    type: 'input',
    keyword: true,
    placeholder: 'SKU / 名称'
  },
  {
    prop: 'categoryId',
    label: '分类',
    type: 'select',
    options: categoryOptions.value
  },
  {
    prop: 'brand',
    label: '品牌',
    type: 'input'
  },
  {
    prop: 'season',
    label: '季节',
    type: 'input'
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  }
])

const columns = computed(() => [
  {
    prop: 'sku',
    label: 'SKU',
    minWidth: 140
  },
  {
    prop: 'name',
    label: '商品名称',
    minWidth: 180
  },
  {
    prop: 'categoryId',
    label: '分类',
    minWidth: 130,
    formatter: categoryLabel
  },
  {
    prop: 'size',
    label: '尺码',
    width: 90
  },
  {
    prop: 'color',
    label: '颜色',
    width: 100
  },
  {
    prop: 'brand',
    label: '品牌',
    minWidth: 120
  },
  {
    prop: 'costPrice',
    label: '成本价',
    width: 110,
    formatter: formatMoney
  },
  {
    prop: 'salePrice',
    label: '销售价',
    width: 110,
    formatter: formatMoney
  },
  {
    prop: 'lowStockThreshold',
    label: '预警阈值',
    width: 110
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    statusOptions: masterStatusOptions
  },
  {
    prop: 'updatedAt',
    label: '更新时间',
    minWidth: 170,
    formatter: formatDateTime
  }
])

const fields = computed(() => [
  {
    prop: 'sku',
    label: 'SKU',
    required: true,
    maxlength: 80
  },
  {
    prop: 'name',
    label: '商品名称',
    required: true,
    maxlength: 150
  },
  {
    prop: 'categoryId',
    label: '分类',
    type: 'select',
    required: true,
    options: categoryOptions.value
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  },
  {
    prop: 'size',
    label: '尺码',
    maxlength: 30
  },
  {
    prop: 'color',
    label: '颜色',
    maxlength: 50
  },
  {
    prop: 'brand',
    label: '品牌',
    maxlength: 100
  },
  {
    prop: 'season',
    label: '季节',
    maxlength: 50
  },
  {
    prop: 'costPrice',
    label: '成本价',
    type: 'number',
    min: 0,
    precision: 2,
    step: 1
  },
  {
    prop: 'salePrice',
    label: '销售价',
    type: 'number',
    min: 0,
    precision: 2,
    step: 1
  },
  {
    prop: 'lowStockThreshold',
    label: '预警阈值',
    type: 'number',
    min: 0
  }
])

onMounted(async () => {
  const result = await categoryApi.page({
    page: 1,
    size: 200,
    status: 'ACTIVE'
  })

  categories.value = result.records || []
})
</script>
