import http from './http'

function cleanParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  )
}

function createCrudApi(basePath) {
  return {
    page(params) {
      return http.get(basePath, {
        params: cleanParams(params)
      })
    },
    detail(id) {
      return http.get(`${basePath}/${id}`)
    },
    create(payload) {
      return http.post(basePath, payload)
    },
    update(id, payload) {
      return http.put(`${basePath}/${id}`, payload)
    },
    remove(id) {
      return http.delete(`${basePath}/${id}`)
    }
  }
}

export const categoryApi = createCrudApi('/categories')
export const productApi = createCrudApi('/products')
export const warehouseApi = createCrudApi('/warehouses')
export const supplierApi = createCrudApi('/suppliers')

export const dashboardApi = {
  summary() {
    return http.get('/dashboard/summary')
  },
  stockTrend(days = 7) {
    return http.get('/dashboard/stock-trend', {
      params: {
        days
      }
    })
  },
  lowStockTop(limit = 10) {
    return http.get('/dashboard/low-stock-top', {
      params: {
        limit
      }
    })
  }
}

export const stockApi = {
  page(params) {
    return http.get('/stock', {
      params: cleanParams(params)
    })
  },
  lowStock(params) {
    return http.get('/stock/low', {
      params: cleanParams(params)
    })
  },
  byProduct(productId) {
    return http.get(`/stock/product/${productId}`)
  }
}

export const stockMovementApi = {
  page(params) {
    return http.get('/stock-movements', {
      params: cleanParams(params)
    })
  },
  detail(id) {
    return http.get(`/stock-movements/${id}`)
  },
  byProduct(productId, params) {
    return http.get(`/stock-movements/product/${productId}`, {
      params: cleanParams(params)
    })
  },
  byWarehouse(warehouseId, params) {
    return http.get(`/stock-movements/warehouse/${warehouseId}`, {
      params: cleanParams(params)
    })
  }
}

function createDocumentApi(basePath) {
  return {
    page(params) {
      return http.get(basePath, {
        params: cleanParams(params)
      })
    },
    detail(id) {
      return http.get(`${basePath}/${id}`)
    },
    create(payload) {
      return http.post(basePath, payload)
    },
    update(id, payload) {
      return http.put(`${basePath}/${id}`, payload)
    },
    confirm(id) {
      return http.post(`${basePath}/${id}/confirm`)
    },
    cancel(id) {
      return http.post(`${basePath}/${id}/cancel`)
    }
  }
}

export const stockInApi = createDocumentApi('/stock-in')
export const stockOutApi = createDocumentApi('/stock-out')

export const operationLogApi = {
  page(params) {
    return http.get('/operation-logs', {
      params: cleanParams(params)
    })
  },
  detail(id) {
    return http.get(`/operation-logs/${id}`)
  }
}
