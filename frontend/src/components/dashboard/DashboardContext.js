import { inject, provide } from 'vue'

const dashboardContextKey = Symbol('dashboard-context')

export function provideDashboardContext(context) {
  provide(dashboardContextKey, context)
}

export function useDashboardContext() {
  const context = inject(dashboardContextKey)
  if (!context) {
    throw new Error('Dashboard context is not available')
  }
  return context
}
