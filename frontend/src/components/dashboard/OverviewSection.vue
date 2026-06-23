<template>
<section
            id="overview"
            class="course-hero scroll-panel"
            :class="{ 'section-active': active }"
          >
            <div class="hero-orbit hero-orbit-one"></div>
            <div class="hero-orbit hero-orbit-two"></div>
            <div class="hero-copy">
              <p class="eyebrow">{{ selectedCourse.courseCode || 'COURSE KNOWLEDGE' }}</p>
              <h1>{{ selectedCourse.courseName }}<span>.</span></h1>
              <p>{{ selectedCourse.description || '暂未填写课程简介。' }}</p>
              <div class="hero-actions">
                <button type="button" class="primary-pill" @click="openMaterialUploader">
                  上传资料 <span>＋</span>
                </button>
                <button type="button" class="outline-pill" @click="openCourseEditor">
                  修改课程 ↗
                </button>
                <button type="button" class="outline-pill danger-pill" @click="confirmCourseDeletion">
                  删除课程
                </button>
              </div>
            </div>
            <div class="hero-stats">
              <div>
                <strong>{{ String(chapters.length).padStart(2, '0') }}</strong>
                <span>chapters</span>
              </div>
              <div>
                <strong>{{ String(overviewStats.materialCount).padStart(2, '0') }}</strong>
                <span>materials</span>
              </div>
              <div>
                <strong>{{ overviewStats.parsedMaterialCount }}</strong>
                <span>parsed</span>
              </div>
              <div>
                <strong>{{ overviewStats.knowledgeItemCount }}</strong>
                <span>knowledge</span>
              </div>
              <div>
                <strong>{{ overviewStats.examQuestionCount }}</strong>
                <span>questions</span>
              </div>
              <div>
                <strong>{{ overviewStats.examMappingCount }}</strong>
                <span>mappings</span>
              </div>
              <div>
                <strong>{{ overviewStats.exportCount }}</strong>
                <span>exports</span>
              </div>
            </div>
            <div v-loading="courseStatsLoading" class="course-type-distribution">
              <div class="type-distribution-heading">
                <strong>资料类型分布</strong>
                <span>{{ overviewStats.materialCount }} 份资料</span>
              </div>
              <div class="type-distribution-list">
                <div v-for="item in materialTypeDistribution" :key="item.type" class="type-distribution-row">
                  <span>{{ item.label }}</span>
                  <div>
                    <i :style="{ width: `${item.percent}%` }"></i>
                  </div>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>
            </div>
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

const {
  courseStatsLoading,
  chapters,
  materials,
  selectedCourse,
  overviewStats,
  materialTypeDistribution,
  confirmCourseDeletion,
  openCourseEditor,
  openMaterialUploader
} = useDashboardContext()
</script>
