import request from './request'

export function listKnowledgeGapReports(courseId, userId) {
  return request.get(`/courses/${courseId}/knowledge-gap-reports`, {
    params: { userId }
  })
}

export function createKnowledgeGapReport(courseId, data) {
  return request.post(`/courses/${courseId}/knowledge-gap-reports`, data, {
    timeout: 120000
  })
}

export function getKnowledgeGapReport(reportId, userId) {
  return request.get(`/knowledge-gap-reports/${reportId}`, {
    params: { userId }
  })
}

export function listKnowledgeGapItems(reportId, userId) {
  return request.get(`/knowledge-gap-reports/${reportId}/items`, {
    params: { userId }
  })
}

export function listPrerequisiteGapHints(courseId, userId) {
  return request.get(`/courses/${courseId}/prerequisite-gap-hints`, {
    params: { userId }
  })
}
