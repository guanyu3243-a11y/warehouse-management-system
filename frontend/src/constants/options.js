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
