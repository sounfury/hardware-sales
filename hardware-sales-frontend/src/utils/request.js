import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

/** 请求拦截器：附加 SA-Token */
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = token
  }
  return config
})

/** 响应拦截器：统一错误处理 */
service.interceptors.response.use(
  async (response) => {
    if (response.config.responseType === 'blob') {
      const contentType = response.headers['content-type'] || ''
      if (contentType.includes('application/json')) {
        const text = await response.data.text()
        const res = JSON.parse(text)
        ElMessage.error(res.msg || '请求失败')
        if (res.code === 401) {
          localStorage.removeItem('token')
          router.push('/login')
        }
        return Promise.reject(new Error(res.msg || '请求失败'))
      }
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      // 401 未登录 → 跳转登录页
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default service
