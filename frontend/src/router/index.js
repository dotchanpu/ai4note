import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/dashboard/DashboardView.vue'

const routes = [
  {
    path: '/',
    name: 'dashboard',
    component: DashboardView
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})

