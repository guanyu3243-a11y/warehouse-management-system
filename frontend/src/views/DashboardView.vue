<template>
  <section class="page dashboard-page">
    <div class="dashboard-heading">
      <div>
        <h1>工作台</h1>
        <p class="text-muted">欢迎回来，{{ authStore.username }}</p>
      </div>
      <ElTag effect="plain" type="success">{{ authStore.roleLabel }}</ElTag>
    </div>

    <div class="overview-grid">
      <div class="page-panel metric-card">
        <span>登录状态</span>
        <strong>在线</strong>
      </div>
      <div class="page-panel metric-card">
        <span>当前用户</span>
        <strong>{{ authStore.username }}</strong>
      </div>
      <div class="page-panel metric-card">
        <span>当前角色</span>
        <strong>{{ authStore.roleLabel }}</strong>
      </div>
    </div>

    <div class="page-panel quick-panel">
      <div class="panel-title">
        <h2>常用入口</h2>
      </div>
      <div class="quick-grid">
        <RouterLink v-for="item in quickLinks" :key="item.path" :to="item.path">
          <ElIcon><component :is="item.icon" /></ElIcon>
          <span>{{ item.title }}</span>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup>
import {
  Collection,
  DataLine,
  Download,
  Goods,
  OfficeBuilding,
  Upload,
  Warning
} from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const quickLinks = [
  {
    title: '商品分类',
    path: '/categories',
    icon: Collection
  },
  {
    title: '服装商品',
    path: '/products',
    icon: Goods
  },
  {
    title: '仓库管理',
    path: '/warehouses',
    icon: OfficeBuilding
  },
  {
    title: '入库管理',
    path: '/stock-in',
    icon: Download
  },
  {
    title: '出库管理',
    path: '/stock-out',
    icon: Upload
  },
  {
    title: '库存查询',
    path: '/stock',
    icon: DataLine
  },
  {
    title: '低库存预警',
    path: '/low-stock',
    icon: Warning
  }
]
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 20px;
}

.dashboard-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-heading h1 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 24px;
}

.dashboard-heading p {
  margin: 6px 0 0;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  display: grid;
  min-height: 108px;
  align-content: center;
  gap: 8px;
  padding: 20px;
}

.metric-card span {
  color: var(--wms-muted);
  font-size: 13px;
}

.metric-card strong {
  overflow: hidden;
  color: var(--wms-ink);
  font-size: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-panel {
  padding: 20px;
}

.panel-title {
  margin-bottom: 16px;
}

.panel-title h2 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 18px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.quick-grid a {
  display: flex;
  min-height: 56px;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--wms-border);
  border-radius: 8px;
  padding: 0 14px;
  background: #fbfcfc;
  color: var(--wms-ink);
}

.quick-grid a:hover {
  border-color: var(--wms-accent);
  color: var(--wms-accent-strong);
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
