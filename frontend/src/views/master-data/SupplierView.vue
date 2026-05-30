<template>
  <MasterCrudPage
    title="供应商管理"
    description="维护供应商编码、联系方式和启停状态。"
    :api="supplierApi"
    :filters="filters"
    :columns="columns"
    :fields="fields"
    :default-form="{ status: 'ACTIVE' }"
  />
</template>

<script setup>
import { supplierApi } from '@/api/business'
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
    label: '供应商编码',
    minWidth: 130
  },
  {
    prop: 'name',
    label: '供应商名称',
    minWidth: 180
  },
  {
    prop: 'contactName',
    label: '联系人',
    minWidth: 110
  },
  {
    prop: 'phone',
    label: '电话',
    minWidth: 130
  },
  {
    prop: 'email',
    label: '邮箱',
    minWidth: 180
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
    label: '供应商编码',
    required: true,
    maxlength: 50
  },
  {
    prop: 'name',
    label: '供应商名称',
    required: true,
    maxlength: 150
  },
  {
    prop: 'contactName',
    label: '联系人',
    maxlength: 50
  },
  {
    prop: 'phone',
    label: '电话',
    maxlength: 30
  },
  {
    prop: 'email',
    label: '邮箱',
    maxlength: 100
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
