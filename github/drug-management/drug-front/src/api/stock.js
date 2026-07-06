import request from '@/utils/request'

// 获取库存列表
export function getStockList(params) {
  return request({
    url: '/stock/list',
    method: 'get',
    params
  })
}

// 获取库存详情
export function getStockDetail(id) {
  return request({
    url: `/stock/${id}`,
    method: 'get'
  })
}

// 库存预警列表
export function getWarningStockList(params) {
  return request({
    url: '/stock/warning/list',
    method: 'get',
    params
  })
}

// 库存盘点
export function checkStock(data) {
  return request({
    url: '/stock/check',
    method: 'post',
    data
  })
}
