<template>
      <aside class="course-sidebar" :class="{ 'course-sidebar-switching': courseSwitching }">
        <div class="sidebar-heading">
          <p>课程列表<span>.</span></p>
          <small>{{ courses.length }} 门课程</small>
        </div>

        <div v-loading="courseLoading" class="course-list">
          <button
            v-for="(course, index) in courses"
            :key="course.id"
            type="button"
            class="course-item"
            :class="{
              active: selectedCourse?.id === course.id,
              'is-switching': courseSwitching && selectedCourse?.id === course.id
            }"
            @click="$emit('select-course', course)"
          >
            <span class="course-number">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="course-item-copy">
              <strong>{{ course.courseName }}</strong>
              <span class="course-tags">
                <small>{{ course.courseCode || '未设置代码' }}</small>
                <small>{{ course.semester || '未设置学期' }}</small>
              </span>
            </span>
            <span class="course-arrow">↗</span>
          </button>

          <div v-if="!courseLoading && courses.length === 0" class="sidebar-empty">
            还没有课程，从第一门开始。
          </div>
        </div>

        <button type="button" class="course-map-button" @click.stop.prevent="router.push('/course-map')">
          <span>课程关系图</span>
          <strong>↗</strong>
        </button>

        <button type="button" class="new-course-button" @click.stop.prevent="openCourseCreator">
          <span>新建课程</span>
          <strong>＋</strong>
        </button>
      </aside>
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'

defineProps({
  courseLoading: {
    type: Boolean,
    default: false
  },
  courses: {
    type: Array,
    default: () => []
  },
  selectedCourse: {
    type: Object,
    default: null
  },
  courseSwitching: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select-course'])

const {
  router,
  openCourseCreator
} = useDashboardContext()
</script>
