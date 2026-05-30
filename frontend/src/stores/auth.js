import { defineStore } from 'pinia'

import { currentPermissionsApi, currentUserApi, loginApi, logoutApi } from '@/api/auth'
import {
  clearAuthStorage,
  getStoredUser,
  getToken,
  setAuthStorage
} from '@/utils/auth-storage'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getStoredUser(),
    roles: [],
    permissions: [],
    warehouseIds: [],
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    username: (state) => state.user?.username || '',
    roleLabel: (state) => {
      const roleMap = {
        ADMIN: '管理员',
        MANAGER: '仓库主管',
        STAFF: '仓库人员'
      }

      return roleMap[state.user?.role] || state.user?.role || '未登录'
    },
    isAdmin: (state) => state.user?.role === 'ADMIN',
    hasPermission: (state) => (code) => {
      if (!code) {
        return true
      }
      if (state.user?.role === 'ADMIN') {
        return true
      }
      return state.permissions.includes(code)
    }
  },
  actions: {
    async login(form) {
      this.loading = true

      try {
        const result = await loginApi(form)

        this.token = result.token
        this.user = result.user
        setAuthStorage(result.token, result.user)
        await this.fetchPermissions()

        return result
      } finally {
        this.loading = false
      }
    },
    async fetchCurrentUser() {
      if (!this.token) {
        return null
      }

      const user = await currentUserApi()

      this.user = user
      setAuthStorage(this.token, user)
      await this.fetchPermissions()

      return user
    },
    async fetchPermissions() {
      if (!this.token) {
        return null
      }

      const result = await currentPermissionsApi()
      this.roles = result.roles || []
      this.permissions = result.permissions || []
      this.warehouseIds = result.warehouseIds || []
      return result
    },
    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      this.token = null
      this.user = null
      this.roles = []
      this.permissions = []
      this.warehouseIds = []
      clearAuthStorage()
    }
  }
})
