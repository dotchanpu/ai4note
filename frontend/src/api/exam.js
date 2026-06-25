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

export function generateAnswer(questionId, userId) {
  return request.post(`/exam-questions/${questionId}/generate-answer`, null, {
    params: { userId },
    timeout: 120000
  })
}

export function generateBatchAnswers(courseId, userId) {
  return request.post(`/courses/${courseId}/exam-questions/generate-answers`, null, {
    params: { userId },
    timeout: 300000
  })
}
