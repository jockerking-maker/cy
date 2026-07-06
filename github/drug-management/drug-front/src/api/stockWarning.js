import request from '@/utils/request'

export function getStockWarningList(params) {
  return request({ url: '/stock-warning/list', method: 'get', params })
}

export function handleWarning(warningId, data) {
  return request({ url: `/stock-warning/handle/${warningId}`, method: 'post', data })
}

export function deleteWarning(warningId) {
  return request({ url: '/stock-warning/delete', method: 'post', data: { warningId } })
}

export function getWarningStats() {
  return request({ url: '/stock-warning/stats', method: 'get' })
}

export function getWarningTrend(months) {
  return request({ url: '/stock-warning/trend', method: 'get', params: { months } })
}

export function assignWarning(data) {
  return request({ url: '/stock-warning/assign', method: 'post', data })
}

export function batchHandleWarnings(data) {
  return request({ url: '/stock-warning/batch-handle', method: 'post', data })
}

export function batchDeleteWarnings(data) {
  return request({ url: '/stock-warning/batch-delete', method: 'post', data })
}

export function checkNearExpiry() {
  return request({ url: '/stock-warning/check-near-expiry', method: 'post' })
}

export function checkSlowMoving() {
  return request({ url: '/stock-warning/check-slow-moving', method: 'post' })
}
