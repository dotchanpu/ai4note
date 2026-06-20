import request from './request'

export function generateSprintOutline(courseId, data) {
  return request.post(`/courses/${courseId}/sprint-outlines/generate`, data, {
    timeout: 180000
  })
}

export function sprintOutlineDownloadUrl(taskId, userId) {
  return `/api/sprint-outlines/${taskId}/download?userId=${encodeURIComponent(userId)}`
}
