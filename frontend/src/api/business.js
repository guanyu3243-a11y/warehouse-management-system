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

function createExcelApi(basePath) {
  return {
    export(params) {
      return http.get(`${basePath}/export`, {
        params: cleanParams(params),
        responseType: 'blob'
      })
    },
    importTemplate() {
      return http.get(`${basePath}/import-template`, {
        responseType: 'blob'
      })
    },
    importFile(file) {
      const formData = new FormData()
      formData.append('file', file)
      return http.post(`${basePath}/import`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
    }
  }
}

function withExcel(api, basePath) {
  return {
    ...api,
    ...createExcelApi(basePath)
  }
}

export const categoryApi = createCrudApi('/categories')
export const productApi = withExcel(createCrudApi('/products'), '/products')
export const warehouseApi = withExcel(createCrudApi('/warehouses'), '/warehouses')
export const supplierApi = withExcel(createCrudApi('/suppliers'), '/suppliers')

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
  },
  export(params) {
    return http.get('/stock/export', {
      params: cleanParams(params),
      responseType: 'blob'
    })
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
  },
  export(params) {
    return http.get('/stock-movements/export', {
      params: cleanParams(params),
      responseType: 'blob'
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
    },
    export(params) {
      return http.get(`${basePath}/export`, {
        params: cleanParams(params),
        responseType: 'blob'
      })
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
