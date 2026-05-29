<template>
  <ElContainer class="app-shell">
    <ElAside class="app-sidebar" :width="isCollapsed ? '72px' : '236px'">
      <div class="brand" :class="{ 'is-collapsed': isCollapsed }">
        <div class="brand-mark">仓</div>
        <div v-if="!isCollapsed" class="brand-copy">
          <strong>服装仓库管理</strong>
          <span>Warehouse Console</span>
        </div>
      </div>

      <ElMenu
        router
        :collapse="isCollapsed"
        :default-active="route.path"
        class="side-menu"
      >
        <ElMenuItem index="/dashboard">
          <ElIcon><House /></ElIcon>
          <template #title>Dashboard</template>
        </ElMenuItem>
        <ElMenuItem index="/categories">
          <ElIcon><Collection /></ElIcon>
          <template #title>商品分类</template>
        </ElMenuItem>
        <ElMenuItem index="/products">
          <ElIcon><Goods /></ElIcon>
          <template #title>服装商品</template>
        </ElMenuItem>
        <ElMenuItem index="/warehouses">
          <ElIcon><OfficeBuilding /></ElIcon>
          <template #title>仓库管理</template>
        </ElMenuItem>
        <ElMenuItem index="/suppliers">
          <ElIcon><Van /></ElIcon>
          <template #title>供应商管理</template>
        </ElMenuItem>
        <ElMenuItem index="/stock-in">
          <ElIcon><Download /></ElIcon>
          <template #title>入库管理</template>
        </ElMenuItem>
        <ElMenuItem index="/stock-out">
          <ElIcon><Upload /></ElIcon>
          <template #title>出库管理</template>
        </ElMenuItem>
        <ElMenuItem index="/stock">
          <ElIcon><DataLine /></ElIcon>
          <template #title>库存查询</template>
        </ElMenuItem>
        <ElMenuItem index="/low-stock">
          <ElIcon><Warning /></ElIcon>
          <template #title>低库存预警</template>
        </ElMenuItem>
        <ElMenuItem index="/operation-logs">
          <ElIcon><Tickets /></ElIcon>
          <template #title>操作日志</template>
        </ElMenuItem>
      </ElMenu>
    </ElAside>

    <ElContainer>
      <ElHeader class="app-header">
        <div class="header-left">
          <ElTooltip :content="isCollapsed ? '展开菜单' : '收起菜单'">
            <ElButton circle text @click="toggleSidebar">
              <ElIcon>
                <Expand v-if="isCollapsed" />
                <Fold v-else />
              </ElIcon>
            </ElButton>
          </ElTooltip>
          <span class="page-title">{{ pageTitle }}</span>
        </div>

        <ElDropdown trigger="click" @command="handleCommand">
          <button class="user-button" type="button">
            <ElAvatar :size="32">{{ avatarText }}</ElAvatar>
            <span class="user-meta">
              <strong>{{ authStore.username }}</strong>
              <small>{{ authStore.roleLabel }}</small>
            </span>
            <ElIcon><ArrowDown /></ElIcon>
          </button>
          <template #dropdown>
            <ElDropdownMenu>
              <ElDropdownItem command="logout">
                <ElIcon><SwitchButton /></ElIcon>
                退出登录
              </ElDropdownItem>
            </ElDropdownMenu>
          </template>
        </ElDropdown>
      </ElHeader>

      <ElMain class="app-main">
        <RouterView />
      </ElMain>
    </ElContainer>
  </ElContainer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Collection,
  DataLine,
  Download,
  Expand,
  Fold,
  Goods,
  House,
  OfficeBuilding,
  SwitchButton,
  Tickets,
  Upload,
  Van,
  Warning
} from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)

const pageTitle = computed(() => route.meta.title || '工作台')
const avatarText = computed(() => authStore.username.slice(0, 1).toUpperCase() || 'U')

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

async function handleCommand(command) {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: hidden;
  border-right: 1px solid var(--wms-border);
  background: #ffffff;
  transition: width 0.2s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid var(--wms-border);
}

.brand.is-collapsed {
  justify-content: center;
  padding: 0;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--wms-accent);
  font-weight: 700;
}

.brand-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.2;
}

.brand-copy strong {
  color: var(--wms-ink);
  font-size: 15px;
}

.brand-copy span {
  color: var(--wms-muted);
  font-size: 12px;
}

.side-menu {
  border-right: 0;
  padding: 8px;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  border-bottom: 1px solid var(--wms-border);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.header-left {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.page-title {
  overflow: hidden;
  color: var(--wms-ink);
  font-size: 17px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-button {
  display: flex;
  max-width: 240px;
  align-items: center;
  gap: 10px;
  border: 0;
  border-radius: 8px;
  padding: 6px 8px;
  color: var(--wms-ink);
  background: transparent;
  cursor: pointer;
}

.user-button:hover {
  background: #eef4f2;
}

.user-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.user-meta strong,
.user-meta small {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta strong {
  font-size: 14px;
}

.user-meta small {
  color: var(--wms-muted);
  font-size: 12px;
}

.app-main {
  min-height: calc(100vh - 64px);
  padding: 0;
  background: #f4f6f5;
}

@media (max-width: 768px) {
  .app-sidebar {
    width: 72px !important;
  }

  .user-meta {
    display: none;
  }
}
</style>
