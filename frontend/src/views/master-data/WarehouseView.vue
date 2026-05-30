<template>
  <MasterCrudPage
    title="仓库管理"
    description="维护仓库编码、地址和联系人信息。"
    :api="warehouseApi"
    :filters="filters"
    :columns="columns"
    :fields="fields"
    :default-form="{ status: 'ACTIVE' }"
  />
</template>

<script setup>
import { warehouseApi } from '@/api/business'
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
    label: '仓库编码',
    minWidth: 130
  },
  {
    prop: 'name',
    label: '仓库名称',
    minWidth: 160
  },
  {
    prop: 'address',
    label: '地址',
    minWidth: 200
  },
  {
    prop: 'contactName',
    label: '联系人',
    minWidth: 110
  },
  {
    prop: 'contactPhone',
    label: '联系电话',
    minWidth: 130
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
]

const fields = [
  {
    prop: 'code',
    label: '仓库编码',
    required: true,
    maxlength: 50
  },
  {
    prop: 'name',
    label: '仓库名称',
    required: true,
    maxlength: 100
  },
  {
    prop: 'contactName',
    label: '联系人',
    maxlength: 50
  },
  {
    prop: 'contactPhone',
    label: '联系电话',
    maxlength: 30
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  },
  {
    prop: 'address',
    label: '地址',
    type: 'textarea',
    wide: true,
    maxlength: 255
  }
]
</script>
