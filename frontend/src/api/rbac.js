import http from './http'

function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )
}

export const roleApi = {
  page(params) {
    return http.get('/roles', {
      params: cleanParams(params)
    })
  },
  detail(id) {
    return http.get(`/roles/${id}`)
  },
  create(payload) {
    return http.post('/roles', payload)
  },
  update(id, payload) {
    return http.put(`/roles/${id}`, payload)
  },
  updateStatus(id, payload) {
    return http.put(`/roles/${id}/status`, payload)
  },
  remove(id) {
    return http.delete(`/roles/${id}`)
  },
  permissions(id) {
    return http.get(`/roles/${id}/permissions`)
  },
  updatePermissions(id, permissionIds) {
    return http.put(`/roles/${id}/permissions`, {
      permissionIds
    })
  }
}

export const permissionApi = {
  list(params) {
    return http.get('/permissions', {
      params: cleanParams(params)
    })
  },
  tree() {
    return http.get('/permissions/tree')
  },
  create(payload) {
    return http.post('/permissions', payload)
  },
  update(id, payload) {
    return http.put(`/permissions/${id}`, payload)
  },
  updateStatus(id, payload) {
    return http.put(`/permissions/${id}/status`, payload)
  }
}
