<template>
          <nav class="section-dots" aria-label="页面章节">
            <button
              v-for="(section, index) in primaryPageSections"
              :key="section.id"
              type="button"
              :class="{ active: activeSection === section.id }"
              :aria-label="section.title"
              @click.stop.prevent="$emit('activate-section', section.id)"
            >
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
            </button>
          </nav>
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
  primaryPageSections
} = useDashboardContext()
</script>
