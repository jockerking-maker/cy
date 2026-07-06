import request from '@/utils/request'

// 获取角色列表
export function getRoleList(params) {
  return request({
    url: '/role/list',
    method: 'get',
    params
  })
}

// 获取角色详情
export function getRoleDetail(id) {
  return request({
    url: `/role/${id}`,
    method: 'get'
  })
}

// 新增角色
export function saveRole(data) {
  return request({
    url: '/role',
    method: 'post',
    data
  })
}

// 修改角色
export function updateRole(data) {
  return request({
    url: '/role',
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(id) {
  return request({
    url: `/role/${id}`,
    method: 'delete'
  })
}

// 分配权限
export function assignPerms(data) {
  return request({
    url: '/role/assignPerms',
    method: 'post',
    data
  })
}

// 获取所有菜单
export function getMenus() {
  return request({
    url: '/role/menus',
    method: 'get'
  })
}

// 根据角色ID获取菜单
export function getMenusByRoleId(roleId) {
  return request({
    url: `/role/menus/${roleId}`,
    method: 'get'
  })
}
