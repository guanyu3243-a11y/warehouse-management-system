<template>
  <StockDocumentView
    type="in"
    title="入库管理"
    description="创建入库草稿、维护入库明细，并在确认后增加库存。"
    :api="stockInApi"
  />
</template>

<script setup>
import { stockInApi } from '@/api/business'
import StockDocumentView from './StockDocumentView.vue'
</script>
