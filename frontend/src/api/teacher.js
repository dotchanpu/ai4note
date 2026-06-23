import request from './request'

export function listTeacherProfiles(courseId, userId) {
  return request.get(`/courses/${courseId}/teacher-profiles`, {
    params: { userId }
  })
}

export function analyzeTeacherProfile(courseId, data) {
  return request.post(`/courses/${courseId}/teacher-profiles/analyze`, data, {
    timeout: 180000
  })
}

export function reanalyzeTeacherProfile(profileId, data) {
  return request.post(`/teacher-profiles/${profileId}/reanalyze`, data, {
    timeout: 180000
  })
}

export function listTeacherProfileEvidence(profileId, userId) {
  return request.get(`/teacher-profiles/${profileId}/evidence`, {
    params: { userId }
  })
}

export function updateTeacherProfile(profileId, data) {
  return request.put(`/teacher-profiles/${profileId}`, data)
}

export function recalculateTeacherProfileConfidence(profileId, userId) {
  return request.put(`/teacher-profiles/${profileId}/confidence-score`, null, {
    params: { userId }
  })
}
