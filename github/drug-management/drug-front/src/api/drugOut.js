import request from '@/utils/request'

// 获取出库单列表
export function getDrugOutList(params) {
  return request({
    url: '/drug/out/list',
    method: 'get',
    params
  })
}

// 获取出库单详情
export function getDrugOutDetail(id) {
  return request({
    url: `/drug/out/${id}`,
    method: 'get'
  })
}

// 新增出库单
export function saveDrugOut(data) {
  return request({
    url: '/drug/out',
    method: 'post',
    data
  })
}

// 删除出库单
export function deleteDrugOut(id) {
  return request({
    url: `/drug/out/${id}`,
    method: 'delete'
  })
}
