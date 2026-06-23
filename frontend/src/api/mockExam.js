import request from './request'

export function generateMockExam(courseId, data) {
  return request.post(`/courses/${courseId}/mock-exams/generate`, data, {
    timeout: 180000
  })
}

export function mockExamDownloadUrl(taskId, userId) {
  return `/api/mock-exams/${taskId}/download?userId=${encodeURIComponent(userId)}`
}
