import request from '@/utils/request'

// 获取采购单列表
export function getPurchaseOrderList(params) {
  return request({
    url: '/purchase/order/list',
    method: 'get',
    params
  })
}

// 获取采购单详情
export function getPurchaseOrderDetail(id) {
  return request({
    url: `/purchase/order/${id}`,
    method: 'get'
  })
}

// 新增采购单
export function savePurchaseOrder(data) {
  return request({
    url: '/purchase/order',
    method: 'post',
    data
  })
}

// 修改采购单
export function updatePurchaseOrder(data) {
  return request({
    url: '/purchase/order',
    method: 'put',
    data
  })
}

// 删除采购单
export function deletePurchaseOrder(id) {
  return request({
    url: `/purchase/order/${id}`,
    method: 'delete'
  })
}

// 批量删除采购单
export function batchDeletePurchaseOrders(data) {
  return request({
    url: '/purchase/order/batch-delete',
    method: 'post',
    data
  })
}

// 审核采购单
export function auditPurchaseOrder(id, data) {
  return request({
    url: `/purchase/order/audit/${id}`,
    method: 'post',
    data
  })
}

// 作废采购单
export function cancelPurchaseOrder(id, data) {
  return request({
    url: `/purchase/order/cancel/${id}`,
    method: 'post',
    data
  })
}
