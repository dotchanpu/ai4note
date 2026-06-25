import request from './request'

export function listReviewProfiles(userId, courseId) {
  return request.get('/review-profiles', {
    params: { userId, courseId }
  })
}

export function createReviewProfile(data) {
  return request.post('/review-profiles', data)
}

export function updateReviewProfile(profileId, data) {
  return request.put(`/review-profiles/${profileId}`, data)
}

export function deleteReviewProfile(profileId, userId) {
  return request.delete(`/review-profiles/${profileId}`, {
    params: { userId }
  })
}

export function generateReviewAsset(courseId, data) {
  return request.post(`/courses/${courseId}/review-assets/generate`, data, {
    timeout: 180000
  })
}

export function reviewAssetDownloadUrl(taskId, userId) {
  return `/api/review-assets/${taskId}/download?userId=${encodeURIComponent(userId)}`
}
