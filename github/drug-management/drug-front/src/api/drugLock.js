import request from '@/utils/request'

// 分页查询锁定记录
export function getDrugLockList(params) {
  return request({
    url: '/drug-lock/list',
    method: 'get',
    params
  })
}

// 锁定药品
export function lockDrug(data) {
  return request({
    url: '/drug-lock/lock',
    method: 'post',
    data
  })
}

// 解锁药品
export function unlockDrug(lockId, unlockUserId) {
  return request({
    url: `/drug-lock/unlock/${lockId}`,
    method: 'post',
    params: { unlockUserId }
  })
}
