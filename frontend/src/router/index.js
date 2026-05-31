import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      public: true,
      title: '登录'
    }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: {
          title: 'Dashboard',
          permission: 'dashboard:view'
        }
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/master-data/CategoryView.vue'),
        meta: {
          title: '商品分类',
          permission: 'category:view'
        }
      },
      {
        path: 'products',
        name: 'products',
        component: () => import('@/views/master-data/ProductView.vue'),
        meta: {
          title: '服装商品',
          permission: 'product:view'
        }
      },
      {
        path: 'warehouses',
        name: 'warehouses',
        component: () => import('@/views/master-data/WarehouseView.vue'),
        meta: {
          title: '仓库管理',
          permission: 'warehouse:view'
        }
      },
      {
        path: 'suppliers',
        name: 'suppliers',
        component: () => import('@/views/master-data/SupplierView.vue'),
        meta: {
          title: '供应商管理',
          permission: 'supplier:view'
        }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/UserList.vue'),
        meta: {
          title: '用户管理',
          adminOnly: true,
          permission: 'user:view'
        }
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/system/RoleList.vue'),
        meta: {
          title: '角色管理',
          adminOnly: true,
          permission: 'role:view'
        }
      },
      {
        path: 'permissions',
        name: 'permissions',
        component: () => import('@/views/system/PermissionList.vue'),
        meta: {
          title: '权限管理',
          adminOnly: true,
          permission: 'permission:view'
        }
      },
      {
        path: 'login-logs',
        name: 'login-logs',
        component: () => import('@/views/logs/LoginLogsView.vue'),
        meta: {
          title: '登录日志',
          adminOnly: true,
          permission: 'login-log:view'
        }
      },
      {
        path: 'change-password',
        name: 'change-password',
        component: () => import('@/views/account/ChangePasswordView.vue'),
        meta: {
          title: '修改密码'
        }
      },
      {
        path: 'stock-in',
        name: 'stock-in',
        component: () => import('@/views/documents/StockInView.vue'),
        meta: {
          title: '入库管理',
          permission: 'stock-in:view'
        }
      },
      {
        path: 'stock-out',
        name: 'stock-out',
        component: () => import('@/views/documents/StockOutView.vue'),
        meta: {
          title: '出库管理',
          permission: 'stock-out:view'
        }
      },
      {
        path: 'inventory-adjustments',
        name: 'inventory-adjustments',
        component: () => import('@/views/stock/InventoryAdjustmentList.vue'),
        meta: {
          title: '库存调整',
          permission: 'inventory-adjustment:view'
        }
      },
      {
        path: 'stock-takes',
        name: 'stock-takes',
        component: () => import('@/views/stock/StockTakeList.vue'),
        meta: {
          title: '库存盘点',
          permission: 'stock-take:view'
        }
      },
      {
        path: 'stock',
        name: 'stock',
        component: () => import('@/views/stock/StockView.vue'),
        meta: {
          title: '库存查询',
          permission: 'stock:view'
        }
      },
      {
        path: 'low-stock',
        name: 'low-stock',
        component: () => import('@/views/stock/StockView.vue'),
        meta: {
          title: '低库存预警',
          permission: 'stock:low:view'
        }
      },
      {
        path: 'stock-movements',
        name: 'stock-movements',
        component: () => import('@/views/stock/StockMovementList.vue'),
        meta: {
          title: '库存流水',
          permission: 'stock-movement:view'
        }
      },
      {
        path: 'operation-logs',
        name: 'operation-logs',
        component: () => import('@/views/logs/OperationLogsView.vue'),
        meta: {
          title: '操作日志',
          permission: 'operation-log:view'
        }
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/error/ForbiddenView.vue'),
        meta: {
          title: '无权限'
        }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'not-found',
        component: () => import('@/views/error/NotFoundView.vue'),
        meta: {
          title: '页面不存在'
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    return authStore.isLoggedIn ? '/dashboard' : true
  }

  if (!authStore.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (!authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      authStore.clearSession()
      return {
        path: '/login',
        query: {
          redirect: to.fullPath
        }
      }
    }
  }

  if (authStore.permissions.length === 0) {
    try {
      await authStore.fetchPermissions()
    } catch {
      authStore.clearSession()
      return {
        path: '/login',
        query: {
          redirect: to.fullPath
        }
      }
    }
  }

  if (to.meta.adminOnly && !authStore.isAdmin) {
    ElMessage.warning('无权限访问该模块')
    return {
      path: '/403',
      query: {
        from: to.fullPath
      }
    }
  }

  if (to.meta.permission && !authStore.hasPermission(to.meta.permission)) {
    ElMessage.warning('无权限访问该模块')
    return {
      path: '/403',
      query: {
        from: to.fullPath
      }
    }
  }

  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '工作台'} - 服装仓库管理系统`
})

export default router
