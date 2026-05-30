import http from './http'

function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )
}

export function pageUsers(params) {
  return http.get('/users', {
    params: cleanParams(params)
  })
}

export function getUser(id) {
  return http.get(`/users/${id}`)
}

export function createUser(payload) {
  return http.post('/users', payload)
}

export function updateUser(id, payload) {
  return http.put(`/users/${id}`, payload)
}

export function updateUserPassword(id, payload) {
  return http.put(`/users/${id}/password`, payload)
}

export function updateUserStatus(id, payload) {
  return http.put(`/users/${id}/status`, payload)
}

export function deleteUser(id) {
  return http.delete(`/users/${id}`)
}

export function getUserRoles(id) {
  return http.get(`/users/${id}/roles`)
}

export function updateUserRoles(id, roleIds) {
  return http.put(`/users/${id}/roles`, {
    roleIds
  })
}

export function getUserWarehouses(id) {
  return http.get(`/users/${id}/warehouses`)
}

export function updateUserWarehouses(id, warehouseIds) {
  return http.put(`/users/${id}/warehouses`, {
    warehouseIds
  })
}
