<template>
    <section class="global-search-shell" :class="{ 'global-search-switching': courseSwitching }">
      <div class="global-search-context">
        <span>current course</span>
        <strong :key="selectedCourse?.id || 'empty'">{{ selectedCourse?.courseName || '请选择课程' }}</strong>
      </div>
      <div class="global-search-box">
        <el-input
          v-model="quickSearchKeyword"
          clearable
          maxlength="255"
          :disabled="!selectedCourse"
          placeholder="搜索资料正文、标签或知识点"
          @keyup.enter="runQuickSearch"
        />
        <button
          type="button"
          class="global-search-submit"
          :disabled="!selectedCourse || searchLoading"
          @click="runQuickSearch"
        >
          {{ searchLoading ? '搜索中' : '搜索' }}
        </button>
        <button
          type="button"
          class="global-search-detail"
          :disabled="!selectedCourse"
          @click="openDetailedSearch"
        >
          详细搜索
        </button>
      </div>
      <p>快速命中当前课程内容；章节、资料类型、重点资料等筛选放在详细搜索里。</p>
    </section>
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'

defineProps({
  selectedCourse: {
    type: Object,
    default: null
  },
  searchLoading: {
    type: Boolean,
    default: false
  },
  courseSwitching: {
    type: Boolean,
    default: false
  }
})

const {
  quickSearchKeyword,
  openDetailedSearch,
  runQuickSearch
} = useDashboardContext()
</script>
