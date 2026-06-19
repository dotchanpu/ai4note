import request from './request'

export function getAiStatus() {
  return request.get('/ai/status')
}

export function chatWithAi(data) {
  return request.post('/ai/chat', data, {
    timeout: 130000
  })
}
