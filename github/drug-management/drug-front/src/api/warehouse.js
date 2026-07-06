import request from '@/utils/request'

// 获取仓库列表
export function getWarehouseList(params) {
  return request({
    url: '/warehouse/list',
    method: 'get',
    params
  })
}

// 获取仓库详情
export function getWarehouseDetail(id) {
  return request({
    url: `/warehouse/${id}`,
    method: 'get'
  })
}

// 新增仓库
export function saveWarehouse(data) {
  return request({
    url: '/warehouse',
    method: 'post',
    data
  })
}

// 修改仓库
export function updateWarehouse(data) {
  return request({
    url: '/warehouse',
    method: 'put',
    data
  })
}

// 删除仓库
export function deleteWarehouse(id) {
  return request({
    url: `/warehouse/${id}`,
    method: 'delete'
  })
}

export function getNextWarehouseCode() {
  return request({
    url: '/warehouse/next-code',
    method: 'get'
  })
}
