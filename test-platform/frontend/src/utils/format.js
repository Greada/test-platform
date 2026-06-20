export function formatDate(row, column, cellValue) {
  if (!cellValue) return '-'
  const d = new Date(cellValue)
  if (isNaN(d.getTime())) return cellValue
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
