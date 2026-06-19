import request from './request'

export function searchMaterials(params) {
  return request.get('/search', { params })
}
