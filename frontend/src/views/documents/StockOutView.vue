<template>
  <StockDocumentView
    type="out"
    title="出库管理"
    description="创建出库草稿、维护出库明细，并在确认前校验库存。"
    :api="stockOutApi"
  />
</template>

<script setup>
import { stockOutApi } from '@/api/business'
import StockDocumentView from './StockDocumentView.vue'
</script>
