<template>
<section
            id="exam"
            class="exam-section scroll-panel"
            :class="{ 'section-active': active }"
          >
            <ExamMappingPanel
              :selected-course="selectedCourse"
              :current-user-id="currentUser?.id"
              :materials="materials"
              :chapters="chapters"
              :knowledge-items="knowledgeItems"
              :preferred-material-id="examPreferredMaterialId"
              @material-parsed="handleExamMaterialParsed"
              @stats-changed="loadCourseStats"
            />
          </section>
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'

defineProps({
  active: {
    type: Boolean,
    default: false
  }
})
import ExamMappingPanel from './ExamMappingPanel.vue'

const {
  currentUser,
  chapters,
  materials,
  knowledgeItems,
  selectedCourse,
  examPreferredMaterialId,
  loadCourseStats,
  handleExamMaterialParsed
} = useDashboardContext()
</script>
