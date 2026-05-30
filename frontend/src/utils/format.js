export function formatDateTime(value) {
  if (!value) {
    return '-'
  }

  return String(value).replace('T', ' ').slice(0, 19)
}

export function formatDateTimeParam(value) {
  if (!value) {
    return undefined
  }

  const date = value instanceof Date ? value : new Date(value)
  const pad = (number) => String(number).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatMoney(value) {
  const number = Number(value || 0)

  return number.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

export function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}
