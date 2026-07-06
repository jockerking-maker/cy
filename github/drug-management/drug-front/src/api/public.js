import request from '@/utils/request'

export function getLoginPageInfo() {
  return request({
    url: '/public/login-info',
    method: 'get'
  })
}
