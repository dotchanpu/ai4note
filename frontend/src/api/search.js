import request from './request'

export function searchMaterials(params) {
  return request.get('/search', { params })
}

export function listSearchRecords(userId, courseId) {
  return request.get('/search/records', {
    params: { userId, courseId }
  })
}
