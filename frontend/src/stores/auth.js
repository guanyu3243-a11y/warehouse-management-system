import { defineStore } from 'pinia'

import { currentUserApi, loginApi, logoutApi } from '@/api/auth'
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
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },
  actions: {
    async login(form) {
      this.loading = true

      try {
        const result = await loginApi(form)

        this.token = result.token
        this.user = result.user
        setAuthStorage(result.token, result.user)

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

      return user
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
      clearAuthStorage()
    }
  }
})
