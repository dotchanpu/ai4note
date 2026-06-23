import { createRouter, createWebHistory } from 'vue-router'
import CourseMapView from '../views/course/CourseMapView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import UserSettingsView from '../views/settings/UserSettingsView.vue'
import {
  DEFAULT_DASHBOARD_SECTION,
  dashboardSectionPath,
  normalizeDashboardSection
} from '../views/dashboard/dashboardPages'

const routes = [
  {
    path: '/',
    redirect: dashboardSectionPath(DEFAULT_DASHBOARD_SECTION)
  },
  {
    path: '/dashboard',
    redirect: dashboardSectionPath(DEFAULT_DASHBOARD_SECTION)
  },
  {
    path: '/dashboard/:section',
    name: 'dashboard-section',
    component: DashboardView
  },
  {
    path: '/course-map',
    name: 'course-map',
    component: CourseMapView
  },
  {
    path: '/settings',
    name: 'settings',
    component: UserSettingsView
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.name !== 'dashboard-section') {
    return true
  }
  const section = normalizeDashboardSection(to.params.section)
  if (section !== to.params.section) {
    return dashboardSectionPath(section)
  }
  return true
})

export default router
