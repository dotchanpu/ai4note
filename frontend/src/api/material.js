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

export function parsePdf(materialId, userId) {
  return request.post(`/materials/${materialId}/parse`, null, {
    params: { userId },
    timeout: 120000
  })
}

export function listTextChunks(materialId, userId) {
  return request.get(`/materials/${materialId}/text-chunks`, {
    params: { userId }
  })
}
