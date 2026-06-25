import axios from 'axios'
import qs from 'qs'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
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
