/**
 * 用户状态管理（Pinia）：Token、用户信息、角色、菜单权限。
 * 登录成功后持久化到 localStorage，供路由守卫与侧边栏渲染使用。
 */
import { defineStore } from 'pinia'
import { login, getCurrentUser } from '@/api/user'

function safeParseJSON(value, fallback) {
  try {
    return value ? JSON.parse(value) : fallback
  } catch {
    return fallback
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: safeParseJSON(localStorage.getItem('userInfo'), {}),
    roles: safeParseJSON(localStorage.getItem('roles'), []),
    menus: safeParseJSON(localStorage.getItem('menus'), [])
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo.username || '',
    realName: (state) => state.userInfo.realName || '',
    userRoles: (state) => state.roles || [],
    userMenus: (state) => state.menus || []
  },
  
  actions: {
    // 登录
    async login(loginForm) {
      try {
        const res = await login(loginForm)
        this.token = res.data.token
        this.userInfo = res.data.userInfo
        this.roles = res.data.roles || []
        this.menus = res.data.menus || []
        
        localStorage.setItem('token', this.token)
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        localStorage.setItem('roles', JSON.stringify(this.roles))
        localStorage.setItem('menus', JSON.stringify(this.menus))
        
        return Promise.resolve(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },
    
    // 获取当前用户信息
    async getCurrentUser() {
      try {
        const res = await getCurrentUser()
        this.userInfo = res.data.userInfo
        this.roles = res.data.roles || []
        this.menus = res.data.menus || []
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        localStorage.setItem('roles', JSON.stringify(this.roles))
        localStorage.setItem('menus', JSON.stringify(this.menus))
        return Promise.resolve(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },
    
    // 登出
    logout() {
      this.token = ''
      this.userInfo = {}
      this.roles = []
      this.menus = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('roles')
      localStorage.removeItem('menus')
    }
  }
})
