import { createRouter, createWebHistory } from 'vue-router'

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
          title: 'Dashboard'
        }
      },
      {
        path: 'categories',
        name: 'categories',
        component: () => import('@/views/master-data/CategoryView.vue'),
        meta: {
          title: '商品分类'
        }
      },
      {
        path: 'products',
        name: 'products',
        component: () => import('@/views/master-data/ProductView.vue'),
        meta: {
          title: '服装商品'
        }
      },
      {
        path: 'warehouses',
        name: 'warehouses',
        component: () => import('@/views/master-data/WarehouseView.vue'),
        meta: {
          title: '仓库管理'
        }
      },
      {
        path: 'suppliers',
        name: 'suppliers',
        component: () => import('@/views/master-data/SupplierView.vue'),
        meta: {
          title: '供应商管理'
        }
      },
      {
        path: 'stock-in',
        name: 'stock-in',
        component: () => import('@/views/documents/StockInView.vue'),
        meta: {
          title: '入库管理'
        }
      },
      {
        path: 'stock-out',
        name: 'stock-out',
        component: () => import('@/views/documents/StockOutView.vue'),
        meta: {
          title: '出库管理'
        }
      },
      {
        path: 'stock',
        name: 'stock',
        component: () => import('@/views/stock/StockView.vue'),
        meta: {
          title: '库存查询'
        }
      },
      {
        path: 'low-stock',
        name: 'low-stock',
        component: () => import('@/views/stock/StockView.vue'),
        meta: {
          title: '低库存预警'
        }
      },
      {
        path: 'operation-logs',
        name: 'operation-logs',
        component: () => import('@/views/logs/OperationLogsView.vue'),
        meta: {
          title: '操作日志'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
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

  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '工作台'} - 服装仓库管理系统`
})

export default router
