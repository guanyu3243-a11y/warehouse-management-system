<template>
  <section class="page dashboard-page">
    <div class="page-header">
      <div>
        <h1>Dashboard</h1>
        <p>查看商品、库存、入库、出库和低库存预警的关键数据。</p>
      </div>
      <ElButton :loading="loading" @click="loadDashboard">
        <ElIcon><Refresh /></ElIcon>
        刷新
      </ElButton>
    </div>

    <div class="overview-grid">
      <div v-for="item in metrics" :key="item.label" class="page-panel metric-card">
        <div class="metric-icon" :class="item.className">
          <ElIcon><component :is="item.icon" /></ElIcon>
        </div>
        <span>{{ item.label }}</span>
        <strong>{{ formatNumber(item.value) }}</strong>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="page-panel trend-panel">
        <div class="panel-title">
          <h2>近 7 天出入库趋势</h2>
        </div>
        <ElTable v-loading="loading" :data="trend" size="small">
          <ElTableColumn prop="date" label="日期" min-width="110" />
          <ElTableColumn prop="stockInQuantity" label="入库数量" min-width="100" />
          <ElTableColumn prop="stockOutQuantity" label="出库数量" min-width="100" />
          <ElTableColumn label="对比" min-width="220">
            <template #default="{ row }">
              <div class="trend-bars">
                <span class="bar in" :style="{ width: trendWidth(row.stockInQuantity) }" />
                <span class="bar out" :style="{ width: trendWidth(row.stockOutQuantity) }" />
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>

      <div class="page-panel low-stock-panel">
        <div class="panel-title">
          <h2>低库存商品</h2>
          <RouterLink to="/low-stock">查看全部</RouterLink>
        </div>
        <ElTable v-loading="loading" :data="lowStockItems" size="small">
          <ElTableColumn prop="sku" label="SKU" min-width="120" show-overflow-tooltip />
          <ElTableColumn prop="productName" label="商品" min-width="150" show-overflow-tooltip />
          <ElTableColumn prop="warehouseName" label="仓库" min-width="120" show-overflow-tooltip />
          <ElTableColumn prop="availableQuantity" label="可用" width="80" />
          <ElTableColumn prop="lowStockThreshold" label="阈值" width="80" />
        </ElTable>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  Box,
  Collection,
  Download,
  Goods,
  OfficeBuilding,
  Refresh,
  Upload,
  Van,
  Warning
} from '@element-plus/icons-vue'

import { dashboardApi } from '@/api/business'
import { formatNumber } from '@/utils/format'

const loading = ref(false)
const summary = ref({})
const trend = ref([])
const lowStockItems = ref([])

const metrics = computed(() => [
  {
    label: '商品总数',
    value: summary.value.productTotal,
    icon: Goods,
    className: 'teal'
  },
  {
    label: '分类总数',
    value: summary.value.categoryTotal,
    icon: Collection,
    className: 'blue'
  },
  {
    label: '仓库总数',
    value: summary.value.warehouseTotal,
    icon: OfficeBuilding,
    className: 'slate'
  },
  {
    label: '供应商总数',
    value: summary.value.supplierTotal,
    icon: Van,
    className: 'green'
  },
  {
    label: '库存总量',
    value: summary.value.totalStockQuantity,
    icon: Box,
    className: 'indigo'
  },
  {
    label: '低库存项',
    value: summary.value.lowStockItemCount,
    icon: Warning,
    className: 'orange'
  },
  {
    label: '今日入库',
    value: summary.value.todayStockInQuantity,
    icon: Download,
    className: 'teal'
  },
  {
    label: '今日出库',
    value: summary.value.todayStockOutQuantity,
    icon: Upload,
    className: 'red'
  }
])

const maxTrendQuantity = computed(() =>
  Math.max(
    1,
    ...trend.value.flatMap((item) => [Number(item.stockInQuantity || 0), Number(item.stockOutQuantity || 0)])
  )
)

function trendWidth(value) {
  const percent = Math.max(6, (Number(value || 0) / maxTrendQuantity.value) * 100)

  return `${percent}%`
}

async function loadDashboard() {
  loading.value = true

  try {
    const [summaryResult, trendResult, lowStockResult] = await Promise.all([
      dashboardApi.summary(),
      dashboardApi.stockTrend(7),
      dashboardApi.lowStockTop(8)
    ])

    summary.value = summaryResult || {}
    trend.value = trendResult || []
    lowStockItems.value = lowStockResult || []
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 20px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 4px 12px;
  align-items: center;
  min-height: 118px;
  padding: 18px;
  border-color: #e4ece9;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 249, 0.96)),
    #ffffff;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease;
}

.metric-card:hover {
  border-color: #cbdad6;
  box-shadow: var(--wms-shadow);
  transform: translateY(-1px);
}

.metric-icon {
  display: grid;
  grid-row: span 2;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  font-size: 20px;
  box-shadow: inset 0 -10px 18px rgba(0, 0, 0, 0.08);
}

.metric-icon.teal {
  background: #0f766e;
}

.metric-icon.blue {
  background: #2563eb;
}

.metric-icon.slate {
  background: #475569;
}

.metric-icon.green {
  background: #15803d;
}

.metric-icon.indigo {
  background: #4f46e5;
}

.metric-icon.orange {
  background: #b45309;
}

.metric-icon.red {
  background: #b42318;
}

.metric-card span {
  color: var(--wms-muted);
  font-size: 13px;
}

.metric-card strong {
  overflow: hidden;
  color: var(--wms-ink);
  font-size: 28px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 18px;
}

.trend-panel,
.low-stock-panel {
  padding: 18px;
  overflow: hidden;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-title h2 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 18px;
  font-weight: 750;
}

.panel-title a {
  color: var(--wms-accent-strong);
  font-size: 14px;
}

.trend-bars {
  display: grid;
  gap: 5px;
}

.bar {
  display: block;
  height: 8px;
  min-width: 8px;
  border-radius: 999px;
}

.bar.in {
  background: #0f766e;
}

.bar.out {
  background: #b7791f;
}

@media (max-width: 1120px) {
  .overview-grid,
  .dashboard-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .overview-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
