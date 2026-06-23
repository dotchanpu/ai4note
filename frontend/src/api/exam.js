import request from './request'

export function extractExamQuestions(materialId, userId, overwrite = false) {
  return request.post(`/materials/${materialId}/exam-questions/extract`, null, {
    params: { userId, overwrite },
    timeout: 300000
  })
}

export function listExamQuestions(courseId, userId, filters = {}) {
  return request.get(`/courses/${courseId}/exam-questions`, {
    params: { userId, ...filters }
  })
}

export function saveExamKnowledgeMap(questionId, userId, data) {
  return request.post(`/exam-questions/${questionId}/knowledge-map`, data, {
    params: { userId }
  })
}

export function listExamKnowledgeStats(courseId, userId, filters = {}) {
  return request.get(`/courses/${courseId}/exam-knowledge-stats`, {
    params: { userId, ...filters }
  })
}

export function listExamKnowledgeTrends(courseId, userId, filters = {}) {
  return request.get(`/courses/${courseId}/exam-knowledge-trends`, {
    params: { userId, ...filters }
  })
}
