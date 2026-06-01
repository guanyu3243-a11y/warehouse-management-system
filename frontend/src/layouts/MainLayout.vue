<template>
  <ElContainer class="app-shell">
    <ElAside class="app-sidebar" :width="isCollapsed ? '72px' : '236px'">
      <div class="brand" :class="{ 'is-collapsed': isCollapsed }">
        <div class="brand-mark">
          <img src="/youbao-logo.jpg" alt="友宝 logo" />
        </div>
        <div v-if="!isCollapsed" class="brand-copy">
          <strong>友宝仓库管理</strong>
        </div>
      </div>

      <ElMenu
        router
        :collapse="isCollapsed"
        :default-active="route.path"
        class="side-menu"
      >
        <ElMenuItem v-if="can('dashboard:view')" index="/dashboard">
          <ElIcon><House /></ElIcon>
          <template #title>Dashboard</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('category:view')" index="/categories">
          <ElIcon><Collection /></ElIcon>
          <template #title>商品分类</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('product:view')" index="/products">
          <ElIcon><Goods /></ElIcon>
          <template #title>服装商品</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('warehouse:view')" index="/warehouses">
          <ElIcon><OfficeBuilding /></ElIcon>
          <template #title>仓库管理</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('supplier:view')" index="/suppliers">
          <ElIcon><Van /></ElIcon>
          <template #title>供应商管理</template>
        </ElMenuItem>
        <ElSubMenu v-if="showSystemMenu" index="/system">
          <template #title>
            <ElIcon><Setting /></ElIcon>
            <span>系统管理</span>
          </template>
          <ElMenuItem v-if="authStore.isAdmin && can('user:view')" index="/users">
            <ElIcon><User /></ElIcon>
            <template #title>用户管理</template>
          </ElMenuItem>
          <ElMenuItem v-if="authStore.isAdmin && can('role:view')" index="/roles">
            <ElIcon><UserFilled /></ElIcon>
            <template #title>角色管理</template>
          </ElMenuItem>
          <ElMenuItem v-if="authStore.isAdmin && can('permission:view')" index="/permissions">
            <ElIcon><Lock /></ElIcon>
            <template #title>权限管理</template>
          </ElMenuItem>
          <ElMenuItem v-if="authStore.isAdmin && can('login-log:view')" index="/login-logs">
            <ElIcon><Clock /></ElIcon>
            <template #title>登录日志</template>
          </ElMenuItem>
        </ElSubMenu>
        <ElMenuItem v-if="can('stock-in:view')" index="/stock-in">
          <ElIcon><Download /></ElIcon>
          <template #title>入库管理</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('stock-out:view')" index="/stock-out">
          <ElIcon><Upload /></ElIcon>
          <template #title>出库管理</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('inventory-adjustment:view')" index="/inventory-adjustments">
          <ElIcon><EditPen /></ElIcon>
          <template #title>库存调整</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('stock-take:view')" index="/stock-takes">
          <ElIcon><Memo /></ElIcon>
          <template #title>库存盘点</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('stock:view')" index="/stock">
          <ElIcon><DataLine /></ElIcon>
          <template #title>库存查询</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('stock:low:view')" index="/low-stock">
          <ElIcon><Warning /></ElIcon>
          <template #title>低库存预警</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('stock-movement:view')" index="/stock-movements">
          <ElIcon><List /></ElIcon>
          <template #title>库存流水</template>
        </ElMenuItem>
        <ElMenuItem v-if="can('operation-log:view')" index="/operation-logs">
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
              <ElDropdownItem command="changePassword">
                <ElIcon><Key /></ElIcon>
                修改密码
              </ElDropdownItem>
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
  Clock,
  Collection,
  DataLine,
  Download,
  EditPen,
  Expand,
  Fold,
  Goods,
  House,
  Key,
  List,
  Lock,
  Memo,
  OfficeBuilding,
  Setting,
  SwitchButton,
  Tickets,
  Upload,
  User,
  UserFilled,
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
const showSystemMenu = computed(
  () => authStore.isAdmin && (can('user:view') || can('role:view') || can('permission:view') || can('login-log:view'))
)

function can(permission) {
  return authStore.hasPermission(permission)
}

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

async function handleCommand(command) {
  if (command === 'changePassword') {
    router.push('/change-password')
    return
  }

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
  border-right: 0;
  background:
    linear-gradient(180deg, rgba(15, 118, 110, 0.18), rgba(15, 118, 110, 0) 220px),
    var(--wms-sidebar);
  box-shadow: 10px 0 32px rgba(18, 32, 31, 0.12);
  transition: width 0.2s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 68px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
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
  overflow: hidden;
  padding: 4px;
  background: #ffffff;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.18);
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.2;
}

.brand-copy strong {
  color: #f8fafc;
  font-size: 15px;
}

.brand-copy span {
  color: var(--wms-sidebar-muted);
  font-size: 12px;
}

.side-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #c7d8d4;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
  --el-menu-active-color: #ffffff;
  border-right: 0;
  padding: 10px 8px;
  background: transparent;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 3px 0;
  border-radius: 8px;
  color: #c7d8d4;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: rgba(15, 118, 110, 0.88);
  box-shadow: 0 10px 20px rgba(15, 118, 110, 0.24);
}

.side-menu :deep(.el-menu--inline) {
  background: transparent;
}

.side-menu :deep(.el-icon) {
  color: inherit;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  border-bottom: 1px solid #e6eeeb;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
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
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-button {
  display: flex;
  max-width: 240px;
  align-items: center;
  gap: 10px;
  border: 1px solid #e6eeeb;
  border-radius: 8px;
  padding: 6px 8px;
  color: var(--wms-ink);
  background: #ffffff;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(18, 32, 31, 0.05);
}

.user-button:hover {
  border-color: #c8d6d2;
  background: #f8faf9;
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
  min-height: calc(100vh - 68px);
  padding: 0;
  background:
    linear-gradient(180deg, rgba(15, 118, 110, 0.045), rgba(255, 255, 255, 0) 260px),
    #f5f7f6;
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
