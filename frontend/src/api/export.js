import request from './request'

export function listExportTemplates() {
  return request.get('/export-templates')
}

export function listExportRecords(userId, courseId) {
  return request.get('/exports', {
    params: { userId, courseId }
  })
}

export function createExport(data) {
  return request.post('/exports', data, {
    timeout: 120000
  })
}

export function previewExport(data) {
  return request.post('/exports/preview', data)
}

export function exportDownloadUrl(exportId, userId) {
  return `/api/exports/${exportId}/download?userId=${encodeURIComponent(userId)}`
}
