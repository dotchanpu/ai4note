import request from './request'

export function listCourses(userId) {
  return request.get('/courses', { params: { userId } })
}

export function createCourse(data) {
  return request.post('/courses', data)
}

export function listChapters(courseId, userId) {
  return request.get(`/courses/${courseId}/chapters`, { params: { userId } })
}

export function createChapter(courseId, userId, data) {
  return request.post(`/courses/${courseId}/chapters`, data, { params: { userId } })
}
