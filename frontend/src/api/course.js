import request from './request'

export function listCourses(userId) {
  return request.get('/courses', { params: { userId } })
}

export function createCourse(data) {
  return request.post('/courses', data)
}

export function updateCourse(courseId, data) {
  return request.put(`/courses/${courseId}`, data)
}

export function deleteCourse(courseId, userId) {
  return request.delete(`/courses/${courseId}`, { params: { userId } })
}

export function getCourseStats(courseId, userId) {
  return request.get(`/courses/${courseId}/stats`, { params: { userId } })
}

export function listChapters(courseId, userId) {
  return request.get(`/courses/${courseId}/chapters`, { params: { userId } })
}

export function createChapter(courseId, userId, data) {
  return request.post(`/courses/${courseId}/chapters`, data, { params: { userId } })
}

export function updateChapter(courseId, chapterId, userId, data) {
  return request.put(`/courses/${courseId}/chapters/${chapterId}`, data, {
    params: { userId }
  })
}

export function deleteChapter(courseId, chapterId, userId) {
  return request.delete(`/courses/${courseId}/chapters/${chapterId}`, {
    params: { userId }
  })
}

export function listCourseRelations(courseId, userId) {
  return request.get(`/courses/${courseId}/relations`, { params: { userId } })
}

export function createCourseRelation(courseId, userId, data) {
  return request.post(`/courses/${courseId}/relations`, data, { params: { userId } })
}

export function deleteCourseRelation(relationId, userId) {
  return request.delete(`/course-relations/${relationId}`, { params: { userId } })
}
