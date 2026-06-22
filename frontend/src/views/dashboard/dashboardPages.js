export const DEFAULT_DASHBOARD_SECTION = 'overview'

export const dashboardSections = [
  { id: 'overview', label: '概览', title: '课程概览' },
  { id: 'chapters', label: '章节', title: '章节路径' },
  { id: 'materials', label: '资料', title: '课程资料' },
  { id: 'search', label: '检索', title: '课程检索' },
  { id: 'knowledge', label: '知识', title: '知识条目' },
  { id: 'gaps', label: '缺口', title: '知识缺口' },
  { id: 'teacher', label: '画像', title: '教师画像' },
  { id: 'review', label: '复习', title: '复习配置' },
  { id: 'exam', label: '真题', title: '真题映射' },
  { id: 'export', label: '导出', title: '知识包导出' }
]

export function normalizeDashboardSection(sectionId) {
  return dashboardSections.some(section => section.id === sectionId)
    ? sectionId
    : DEFAULT_DASHBOARD_SECTION
}

export function dashboardSectionPath(sectionId) {
  return `/dashboard/${normalizeDashboardSection(sectionId)}`
}
