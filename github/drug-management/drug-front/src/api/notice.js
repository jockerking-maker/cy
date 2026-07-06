import request from '@/utils/request'

/**
 * 获取公告列表
 */
export function getNoticeList() {
  return request({
    url: '/notice/list',
    method: 'get'
  })
}

/**
 * 新建公告
 */
export function saveNotice(data) {
  return request({
    url: '/notice',
    method: 'post',
    data
  })
}

/**
 * 编辑公告
 */
export function updateNotice(data) {
  return request({
    url: '/notice',
    method: 'put',
    data
  })
}

/**
 * 删除公告
 */
export function deleteNotice(id) {
  return request({
    url: `/notice/${id}`,
    method: 'delete'
  })
}
