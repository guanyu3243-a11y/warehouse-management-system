import axios from 'axios'
import { ElMessage } from 'element-plus'

import { clearAuthStorage, getToken } from '@/utils/auth-storage'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

function redirectToForbidden() {
  if (!['/login', '/403'].includes(window.location.pathname)) {
    window.location.assign('/403')
  }
}

http.interceptors.request.use((config) => {
  const token = getToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (payload && Object.prototype.hasOwnProperty.call(payload, 'code')) {
      if (payload.code === 200) {
        return payload.data
      }

      if (payload.code === 401) {
        clearAuthStorage()
        redirectToLogin()
      }

      if (payload.code === 403) {
        redirectToForbidden()
      }

      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || '请求失败'))
    }

    return payload
  },
  (error) => {
    const status = error.response?.status
    const message =
      error.response?.data?.message ||
      (status ? `请求失败（${status}）` : '网络异常，请检查后端服务')

    if (status === 401) {
      clearAuthStorage()
      redirectToLogin()
    }

    if (status === 403) {
      redirectToForbidden()
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
