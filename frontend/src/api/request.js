import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.message || error.message || '请求失败'
    const wrappedError = new Error(message)
    wrappedError.code = error.response?.data?.code || null
    wrappedError.status = error.response?.status || null
    return Promise.reject(wrappedError)
  }
)

export default request
