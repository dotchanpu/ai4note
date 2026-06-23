import request from './request'

export function getAiStatus() {
  return request.get('/ai/status')
}

export function chatWithAi(data) {
  return request.post('/ai/chat', data, {
    timeout: 130000
  })
}

export function listAiProviders(userId) {
  return request.get('/ai/providers', {
    params: { userId }
  })
}

export function createAiProvider(data) {
  return request.post('/ai/providers', data)
}

export function updateAiProvider(configId, data) {
  return request.put(`/ai/providers/${configId}`, data)
}

export function deleteAiProvider(configId, userId) {
  return request.delete(`/ai/providers/${configId}`, {
    params: { userId }
  })
}

export function listAiGenerationTasks(userId, courseId) {
  return request.get('/ai-generation-tasks', {
    params: {
      userId,
      courseId
    }
  })
}

export function updateAiGenerationTaskStatus(taskId, data) {
  return request.put(`/ai-generation-tasks/${taskId}/status`, data)
}
