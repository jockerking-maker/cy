import request from '@/utils/request'

// 获取供应商列表
export function getSupplierList(params) {
  return request({
    url: '/supplier/list',
    method: 'get',
    params
  })
}

// 获取供应商详情
export function getSupplierDetail(id) {
  return request({
    url: `/supplier/${id}`,
    method: 'get'
  })
}

// 新增供应商
export function saveSupplier(data) {
  return request({
    url: '/supplier',
    method: 'post',
    data
  })
}

// 修改供应商
export function updateSupplier(data) {
  return request({
    url: '/supplier',
    method: 'put',
    data
  })
}

// 删除供应商
export function deleteSupplier(id) {
  return request({
    url: `/supplier/${id}`,
    method: 'delete'
  })
}
