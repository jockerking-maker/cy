import request from '@/utils/request'

// 获取药品列表
export function getDrugList(params) {
  return request({
    url: '/drug/list',
    method: 'get',
    params
  })
}

// 获取下一个药品编码与批准文号
export function getNextDrugCode(params) {
  return request({
    url: '/drug/next-code',
    method: 'get',
    params
  })
}

// 获取药品详情
export function getDrugDetail(id) {
  return request({
    url: `/drug/${id}`,
    method: 'get'
  })
}

// 新增药品
export function saveDrug(data) {
  return request({
    url: '/drug',
    method: 'post',
    data
  })
}

// 修改药品
export function updateDrug(data) {
  return request({
    url: '/drug',
    method: 'put',
    data
  })
}

// 删除药品
export function deleteDrug(id) {
  return request({
    url: `/drug/${id}`,
    method: 'delete'
  })
}

// 更新药品预警值
export function updateDrugWarning(data) {
  return request({
    url: '/drug/warning',
    method: 'put',
    data
  })
}
