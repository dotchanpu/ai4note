import request from './request'

export function listCourseTags(courseId, userId) {
  return request.get(`/courses/${courseId}/tags`, { params: { userId } })
}

export function listMaterialTags(materialId, userId) {
  return request.get(`/materials/${materialId}/tags`, { params: { userId } })
}

export function replaceMaterialTags(materialId, userId, tagNames) {
  return request.put(`/materials/${materialId}/tags`, { tagNames }, {
    params: { userId }
  })
}

export function listKnowledgeItems(courseId, userId, filters = {}) {
  return request.get(`/courses/${courseId}/knowledge-items`, {
    params: { userId, ...filters }
  })
}

export function deleteKnowledgeItem(courseId, itemId, userId) {
  return request.delete(`/courses/${courseId}/knowledge-items/${itemId}`, {
    params: { userId }
  })
}

export function getKnowledgeMastery(knowledgeItemId, userId) {
  return request.get(`/knowledge-items/${knowledgeItemId}/mastery`, {
    params: { userId }
  })
}

export function updateKnowledgeMastery(knowledgeItemId, data) {
  return request.put(`/knowledge-items/${knowledgeItemId}/mastery`, data)
}

export function generateKnowledgeItems(materialId, userId, data) {
  return request.post(`/materials/${materialId}/knowledge-items/ai-generate`, data, {
    params: { userId },
    timeout: 180000
  })
}
