import request from '@/utils/request'

export function getStockCheckList(params) {
  return request({
    url: '/stock-check/list',
    method: 'get',
    params
  })
}

export function getStockCheckDetail(id) {
  return request({
    url: `/stock-check/${id}`,
    method: 'get'
  })
}

export function saveStockCheck(data) {
  return request({
    url: '/stock-check',
    method: 'post',
    data
  })
}

export function completeStockCheck(data) {
  return request({
    url: '/stock-check/complete',
    method: 'put',
    data
  })
}

export function cancelStockCheck(id) {
  return request({
    url: `/stock-check/cancel/${id}`,
    method: 'put'
  })
}

export function deleteStockCheck(id) {
  return request({
    url: `/stock-check/${id}`,
    method: 'delete'
  })
}
