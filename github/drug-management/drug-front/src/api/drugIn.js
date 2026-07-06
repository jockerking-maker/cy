import request from '@/utils/request'

export function getDrugInList(params) {
  return request({
    url: '/drug/in/list',
    method: 'get',
    params
  })
}

export function getDrugInDetail(id) {
  return request({
    url: `/drug/in/${id}`,
    method: 'get'
  })
}

export function saveDrugIn(data) {
  return request({
    url: '/drug/in',
    method: 'post',
    data
  })
}

export function deleteDrugIn(id) {
  return request({
    url: `/drug/in/${id}`,
    method: 'delete'
  })
}

export function getNextBatchNo() {
  return request({
    url: '/drug/in/next-batch-no',
    method: 'get'
  })
}

export function replenishDrugIn(data) {
  return request({
    url: '/drug/in/replenish',
    method: 'post',
    data
  })
}
