import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('exam-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(response => {
  if (response.config.responseType === 'blob') return response
  const result = response.data
  if (result.code !== 200) {
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message))
  }
  return result.data
}, async error => {
  if (error.response?.status === 401) {
    localStorage.removeItem('exam-token')
    localStorage.removeItem('exam-user')
    location.href = '/login'
  }
  let message = error.response?.data?.message
  if (error.response?.data instanceof Blob) {
    try {
      const text = await error.response.data.text()
      message = JSON.parse(text).message
    } catch {
      message = ''
    }
  }
  ElMessage.error(message || error.message || '网络错误')
  return Promise.reject(error)
})

export default request
