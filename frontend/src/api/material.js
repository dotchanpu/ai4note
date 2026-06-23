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

export function uploadMaterialsBatch(data) {
  return request.post('/materials/batch', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000
  })
}

export function updateMaterial(materialId, userId, data) {
  return request.put(`/materials/${materialId}`, data, {
    params: { userId }
  })
}

export function deleteMaterial(materialId, userId) {
  return request.delete(`/materials/${materialId}`, {
    params: { userId }
  })
}

export function parseMaterial(materialId, userId) {
  return request.post(`/materials/${materialId}/parse`, null, {
    params: { userId },
    timeout: 120000
  })
}

export const parsePdf = parseMaterial

export function generateMaterialSummary(materialId, userId, data = {}) {
  return request.post(`/materials/${materialId}/summary/ai-generate`, data, {
    params: { userId },
    timeout: 120000
  })
}

export function listTextChunks(materialId, userId) {
  return request.get(`/materials/${materialId}/text-chunks`, {
    params: { userId }
  })
}

export function previewMaterialContent(materialId, userId) {
  return request.get(`/materials/${materialId}/preview`, {
    params: { userId },
    responseType: 'text',
    transformResponse: [data => data]
  })
}

export function materialFileUrl(materialId, userId, disposition = 'inline') {
  const params = new URLSearchParams({
    userId: String(userId),
    disposition
  })
  return `/api/materials/${materialId}/file?${params.toString()}`
}

export function listSimilarMaterials(materialId, userId) {
  return request.get(`/materials/${materialId}/similar`, {
    params: { userId }
  })
}
