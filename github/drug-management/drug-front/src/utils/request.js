/**
 * Axios 封装：统一 baseURL、请求/响应拦截。
 * <p>
 * - 请求：自动附加 Bearer Token
 * - 响应：401 跳转登录，403 提示权限不足
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.logout()
  router.push('/login')
}

service.interceptors.response.use(
  response => {
    const res = response.data

    if (res.code !== 200) {
      if (res.code === 401) {
        ElMessage.error(res.msg || '登录已过期，请重新登录')
        handleUnauthorized()
      } else {
        ElMessage.error(res.msg || '请求失败')
      }

      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    return res
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        handleUnauthorized()
      } else if (status === 403) {
        ElMessage.error('权限不足，无法访问该资源')
      } else {
        ElMessage.error(error.response.data?.msg || error.message || '请求失败')
      }
    } else {
      ElMessage.error(error.message || '网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service
