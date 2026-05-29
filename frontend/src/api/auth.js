import http from './http'

export function loginApi(payload) {
  return http.post('/auth/login', payload)
}

export function currentUserApi() {
  return http.get('/auth/me')
}

export function logoutApi() {
  return http.post('/auth/logout')
}
