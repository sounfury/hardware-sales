export function formatMoney(value) {
  if (value == null || value === '') return '-'
  const amount = Number(value)
  if (Number.isNaN(amount)) return '-'
  return `¥${amount.toFixed(2)}`
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
