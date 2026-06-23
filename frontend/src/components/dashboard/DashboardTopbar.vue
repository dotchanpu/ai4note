<template>
    <header class="topbar">
      <div class="wordmark workspace-wordmark">ai4note<span>.</span></div>
      <nav class="topnav">
        <button
          v-for="section in primaryPageSections"
          :key="section.id"
          type="button"
          :class="{ active: activeSection === section.id }"
          @click.stop.prevent="$emit('activate-section', section.id)"
        >
          {{ section.label }}
        </button>
      </nav>
      <div class="account-area">
        <button type="button" class="account-user" title="个人设置" @click.stop.prevent="router.push('/settings')">
          <span class="account-dot"></span>
          <span>{{ currentUser.username }}</span>
        </button>
        <button type="button" class="plain-action" @click="logout">退出</button>
      </div>
    </header>
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'

defineProps({
  activeSection: {
    type: String,
    default: 'overview'
  }
})

defineEmits(['activate-section'])

const {
  router,
  currentUser,
  primaryPageSections,
  logout
} = useDashboardContext()
</script>
