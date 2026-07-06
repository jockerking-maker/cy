import request from '@/utils/request'

/**
 * 获取首页统计数据
 */
export function getDashboardStats() {
  return request({
    url: '/dashboard/stats',
    method: 'get'
  })
}

export function getDrugTypeStats() {
  return request({ url: '/dashboard/drugTypeStats', method: 'get' })
}

export function getStockWarningStats() {
  return request({ url: '/dashboard/stockWarningStats', method: 'get' })
}
