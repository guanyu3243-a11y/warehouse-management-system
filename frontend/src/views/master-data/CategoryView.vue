<template>
  <MasterCrudPage
    title="商品分类"
    description="维护服装商品的分类编码、排序和启停状态。"
    :api="categoryApi"
    :filters="filters"
    :columns="columns"
    :fields="fields"
    :default-form="{ status: 'ACTIVE', sortOrder: 0 }"
  />
</template>

<script setup>
import { categoryApi } from '@/api/business'
import { masterStatusOptions } from '@/constants/options'
import { formatDateTime } from '@/utils/format'
import MasterCrudPage from './MasterCrudPage.vue'

const filters = [
  {
    prop: 'keyword',
    label: '关键词',
    type: 'input',
    keyword: true,
    placeholder: '名称 / 编码'
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  }
]

const columns = [
  {
    prop: 'code',
    label: '分类编码',
    minWidth: 130
  },
  {
    prop: 'name',
    label: '分类名称',
    minWidth: 160
  },
  {
    prop: 'sortOrder',
    label: '排序',
    width: 90
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    statusOptions: masterStatusOptions
  },
  {
    prop: 'description',
    label: '说明',
    minWidth: 180
  },
  {
    prop: 'updatedAt',
    label: '更新时间',
    minWidth: 170,
    formatter: formatDateTime
  }
]

const fields = [
  {
    prop: 'code',
    label: '分类编码',
    required: true,
    maxlength: 50
  },
  {
    prop: 'name',
    label: '分类名称',
    required: true,
    maxlength: 100
  },
  {
    prop: 'sortOrder',
    label: '排序',
    type: 'number',
    min: 0
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  },
  {
    prop: 'description',
    label: '说明',
    type: 'textarea',
    wide: true,
    maxlength: 255
  }
]
</script>
