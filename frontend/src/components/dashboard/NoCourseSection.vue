<template>
        <section class="no-course">
          <div class="no-course-shape"></div>
          <p class="eyebrow">第一门课程</p>
          <h1>从一门课程开始<span>.</span></h1>
          <button type="button" class="primary-pill" @click="openCourseCreator">新建课程 ＋</button>
        </section>
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'

const {
  openCourseCreator
} = useDashboardContext()
</script>
