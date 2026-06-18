import request from './request'

export function listMaterials(courseId, userId) {
  return request.get(`/courses/${courseId}/materials`, { params: { userId } })
}

export function uploadMaterial(data) {
  return request.post('/materials', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
