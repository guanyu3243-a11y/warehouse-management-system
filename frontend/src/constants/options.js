export const masterStatusOptions = [
  {
    label: '启用',
    value: 'ACTIVE',
    type: 'success'
  },
  {
    label: '停用',
    value: 'DISABLED',
    type: 'info'
  }
]

export const documentStatusOptions = [
  {
    label: '草稿',
    value: 'DRAFT',
    type: 'warning'
  },
  {
    label: '已确认',
    value: 'CONFIRMED',
    type: 'success'
  },
  {
    label: '已取消',
    value: 'CANCELLED',
    type: 'info'
  }
]

export const operationModuleOptions = [
  'AUTH',
  'CATEGORY',
  'PRODUCT',
  'WAREHOUSE',
  'SUPPLIER',
  'STOCK_IN',
  'STOCK_OUT',
  'STOCK',
  'STOCK_MOVEMENT',
  'DASHBOARD',
  'OPERATION_LOG'
]

export const operationActionOptions = [
  'REGISTER',
  'LOGIN',
  'LOGOUT',
  'CREATE',
  'UPDATE',
  'DELETE',
  'CONFIRM',
  'CANCEL'
]

export function findOption(options, value) {
  return options.find((option) => option.value === value)
}

export function statusLabel(options, value) {
  return findOption(options, value)?.label || value || '-'
}

export function statusType(options, value) {
  return findOption(options, value)?.type || 'info'
}

export const stockMovementTypeOptions = [
  {
    label: '入库',
    value: 'STOCK_IN',
    type: 'success'
  },
  {
    label: '出库',
    value: 'STOCK_OUT',
    type: 'warning'
  },
  {
    label: '库存调整',
    value: 'ADJUSTMENT',
    type: 'primary'
  },
  {
    label: '调拨入库',
    value: 'TRANSFER_IN',
    type: 'success'
  },
  {
    label: '调拨出库',
    value: 'TRANSFER_OUT',
    type: 'warning'
  },
  {
    label: '库存盘点',
    value: 'STOCK_TAKE',
    type: 'info'
  }
]
