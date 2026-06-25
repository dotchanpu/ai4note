<template>
  <main class="course-map-page">
    <header class="map-topbar">
      <button type="button" class="back-button" @click="goDashboard">← 返回工作台</button>
      <div>
        <p class="eyebrow">course relation map</p>
        <h1>课程关系图<span>.</span></h1>
      </div>
      <div v-if="currentUser" class="map-account">
        <span class="account-dot"></span>
        <strong>{{ currentUser.username }}</strong>
      </div>
    </header>

    <section v-if="!currentUser" class="empty-state">
      <strong>还没有登录。</strong>
      <p>请先返回工作台登录，再查看课程关系图。</p>
      <button type="button" @click="goDashboard">去登录</button>
    </section>

    <section v-else class="map-workspace">
      <aside class="map-sidebar">
        <div class="sidebar-card identity-card">
          <p class="eyebrow">all courses</p>
          <strong>{{ courses.length }}</strong>
          <span>门课程正在地图中等待连接</span>
        </div>

        <form class="relation-builder" @submit.prevent="saveRelation">
          <div>
            <p class="eyebrow">new relation</p>
            <h2>添加一条关系</h2>
          </div>

          <label>
            <span>当前课程</span>
            <el-select v-model="relationForm.courseId" filterable placeholder="选择课程">
              <el-option
                v-for="course in courses"
                :key="course.id"
                :label="courseLabel(course)"
                :value="course.id"
              />
            </el-select>
          </label>

          <label>
            <span>关系类型</span>
            <el-select v-model="relationForm.relationType">
              <el-option
                v-for="option in relationTypeOptions"
                :key="option.type"
                :label="option.label"
                :value="option.type"
              />
            </el-select>
          </label>

          <label>
            <span>{{ relatedCourseFieldLabel }}</span>
            <el-select v-model="relationForm.relatedCourseId" filterable placeholder="选择关联课程">
              <el-option
                v-for="course in relationCandidateCourses"
                :key="course.id"
                :label="courseLabel(course)"
                :value="course.id"
              />
            </el-select>
          </label>

          <label>
            <span>说明</span>
            <el-input
              v-model="relationForm.reason"
              maxlength="255"
              placeholder="例如：学习图算法前建议先掌握指针和结构体"
            />
          </label>

          <button type="submit" :disabled="relationSaving || !canSaveRelation">
            {{ relationSaving ? '保存中…' : '添加到地图' }}
          </button>
        </form>

        <section class="course-list-panel">
          <div class="panel-heading">
            <strong>课程节点</strong>
            <button type="button" @click="selectedCourseId = null">全部</button>
          </div>
          <div v-loading="courseLoading" class="course-node-list">
            <button
              v-for="course in courses"
              :key="course.id"
              type="button"
              :class="{ active: selectedCourseId === course.id }"
              @click="selectCourse(course.id)"
            >
              <span>{{ degreeOf(course.id) }}</span>
              <strong>{{ course.courseName }}</strong>
              <small>{{ course.courseCode || 'COURSE' }} · {{ course.semester || '未设置学期' }}</small>
            </button>
          </div>
        </section>
      </aside>

      <section class="map-main">
        <section class="map-hero">
          <div>
            <p class="eyebrow">visual graph</p>
            <h2>把课程依赖看成一张地图</h2>
            <p>
              前置关系会画成「前置课 → 当前课」，后续关系画成「当前课 → 后续课」。
              点击左侧课程可以聚焦它的相关连线。
            </p>
          </div>
          <div class="map-stats">
            <article>
              <span>课程</span>
              <strong>{{ courses.length }}</strong>
            </article>
            <article>
              <span>前置</span>
              <strong>{{ relationStats.prerequisite }}</strong>
            </article>
            <article>
              <span>关联</span>
              <strong>{{ relationStats.related }}</strong>
            </article>
            <article>
              <span>后续</span>
              <strong>{{ relationStats.followUp }}</strong>
            </article>
          </div>
        </section>

        <section class="graph-shell">
          <div class="graph-toolbar">
            <div class="legend">
              <span class="legend-prerequisite">前置</span>
              <span class="legend-related">关联</span>
              <span class="legend-follow-up">后续</span>
            </div>
            <button type="button" :disabled="loading" @click="reloadData">
              {{ loading ? '刷新中…' : '刷新地图' }}
            </button>
          </div>
          <div ref="chartEl" class="relation-chart"></div>
          <div v-if="!loading && courses.length === 0" class="graph-empty">
            还没有课程，先回到工作台创建第一门课。
          </div>
        </section>

        <section class="relation-list-section">
          <div class="section-heading">
            <div>
              <p class="eyebrow">relations</p>
              <h2>{{ selectedCourse ? selectedCourse.courseName : '全部关系' }}</h2>
            </div>
            <span>{{ visibleRelations.length }} 条</span>
          </div>

          <div v-loading="relationLoading" class="relation-card-grid">
            <article v-for="relation in visibleRelations" :key="relation.id" class="relation-map-card">
              <div class="relation-map-card-top">
                <span :class="`type-${relation.relationType}`">{{ relationTypeLabel(relation.relationType) }}</span>
                <button type="button" @click="removeRelation(relation)">删除</button>
              </div>
              <div class="relation-route">
                <strong>{{ routeStartLabel(relation) }}</strong>
                <i>→</i>
                <strong>{{ routeEndLabel(relation) }}</strong>
              </div>
              <p>{{ relation.reason || '暂无关系说明' }}</p>
              <small>{{ courseCodeLabel(relation.courseId) }} / {{ courseCodeLabel(relation.relatedCourseId) }}</small>
            </article>

            <div v-if="!relationLoading && visibleRelations.length === 0" class="empty-card">
              <strong>还没有关系。</strong>
              <p>用左侧表单添加一条前置、关联或后续关系，地图会立刻更新。</p>
            </div>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  createCourseRelation,
  deleteCourseRelation,
  listAllCourseRelations,
  listCourses
} from '../../api/course'

const router = useRouter()
const currentUser = ref(null)
const courses = ref([])
const relations = ref([])
const selectedCourseId = ref(null)
const courseLoading = ref(false)
const relationLoading = ref(false)
const relationSaving = ref(false)
const chartEl = ref(null)
let chart = null

const relationTypeOptions = [
  { type: 'PREREQUISITE', label: '前置课程', color: '#ff3151' },
  { type: 'RELATED', label: '关联课程', color: '#14cbea' },
  { type: 'FOLLOW_UP', label: '后续课程', color: '#0aaf86' }
]

const relationForm = reactive({
  courseId: null,
  relatedCourseId: null,
  relationType: 'PREREQUISITE',
  reason: ''
})

const loading = computed(() => courseLoading.value || relationLoading.value)

const courseMap = computed(() => {
  const map = new Map()
  courses.value.forEach(course => map.set(course.id, course))
  return map
})

const selectedCourse = computed(() => (
  selectedCourseId.value ? courseMap.value.get(selectedCourseId.value) : null
))

const visibleRelations = computed(() => {
  if (!selectedCourseId.value) {
    return relations.value
  }
  return relations.value.filter(relation => (
    relation.courseId === selectedCourseId.value || relation.relatedCourseId === selectedCourseId.value
  ))
})

const relationCandidateCourses = computed(() => {
  if (!relationForm.courseId) {
    return courses.value
  }
  const relatedIds = new Set(
    relations.value
      .filter(relation => relation.courseId === relationForm.courseId)
      .map(relation => relation.relatedCourseId)
  )
  return courses.value.filter(course => (
    course.id !== relationForm.courseId && !relatedIds.has(course.id)
  ))
})

const canSaveRelation = computed(() => (
  relationForm.courseId
    && relationForm.relatedCourseId
    && relationForm.courseId !== relationForm.relatedCourseId
))

const relatedCourseFieldLabel = computed(() => {
  if (relationForm.relationType === 'PREREQUISITE') return '前置课程'
  if (relationForm.relationType === 'FOLLOW_UP') return '后续课程'
  return '关联课程'
})

const relationStats = computed(() => ({
  prerequisite: relations.value.filter(relation => relation.relationType === 'PREREQUISITE').length,
  related: relations.value.filter(relation => relation.relationType === 'RELATED').length,
  followUp: relations.value.filter(relation => relation.relationType === 'FOLLOW_UP').length
}))

onMounted(async () => {
  const savedUser = localStorage.getItem('ai4note-user')
  if (!savedUser) {
    return
  }
  currentUser.value = JSON.parse(savedUser)
  await reloadData()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(
  [courses, relations, selectedCourseId],
  () => {
    nextTick(renderChart)
  },
  { deep: true }
)

watch(
  () => relationForm.courseId,
  () => {
    if (relationForm.relatedCourseId === relationForm.courseId) {
      relationForm.relatedCourseId = null
    }
    if (!relationCandidateCourses.value.some(course => course.id === relationForm.relatedCourseId)) {
      relationForm.relatedCourseId = null
    }
  }
)

function goDashboard() {
  router.push('/dashboard/overview')
}

async function reloadData() {
  if (!currentUser.value) return
  courseLoading.value = true
  relationLoading.value = true
  try {
    const [courseData, relationData] = await Promise.all([
      listCourses(currentUser.value.id),
      listAllCourseRelations(currentUser.value.id)
    ])
    courses.value = courseData
    relations.value = relationData
    if (!relationForm.courseId && courseData.length > 0) {
      relationForm.courseId = courseData[0].id
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseLoading.value = false
    relationLoading.value = false
  }
}

async function saveRelation() {
  if (!canSaveRelation.value || !currentUser.value) {
    ElMessage.warning('请选择两门不同课程')
    return
  }
  relationSaving.value = true
  try {
    const relation = await createCourseRelation(relationForm.courseId, currentUser.value.id, {
      relatedCourseId: relationForm.relatedCourseId,
      relationType: relationForm.relationType,
      reason: relationForm.reason.trim() || null,
      sortOrder: relations.value.filter(item => item.courseId === relationForm.courseId).length
    })
    relations.value = [...relations.value, relation]
    selectedCourseId.value = relationForm.courseId
    relationForm.relatedCourseId = null
    relationForm.reason = ''
    ElMessage.success('课程关系已添加')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    relationSaving.value = false
  }
}

async function removeRelation(relation) {
  if (!currentUser.value) return
  try {
    await deleteCourseRelation(relation.id, currentUser.value.id)
    relations.value = relations.value.filter(item => item.id !== relation.id)
    ElMessage.success('课程关系已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function selectCourse(courseId) {
  selectedCourseId.value = selectedCourseId.value === courseId ? null : courseId
  relationForm.courseId = courseId
}

function renderChart() {
  if (!chartEl.value || courses.value.length === 0) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartEl.value)
    chart.on('click', params => {
      if (params.dataType === 'node') {
        selectCourse(Number(params.data.id))
      }
    })
  }

  const selectedId = selectedCourseId.value
  const activeNodeIds = new Set()
  if (selectedId) {
    activeNodeIds.add(selectedId)
    visibleRelations.value.forEach(relation => {
      activeNodeIds.add(relation.courseId)
      activeNodeIds.add(relation.relatedCourseId)
    })
  }

  const nodes = courses.value.map(course => {
    const degree = degreeOf(course.id)
    const active = !selectedId || activeNodeIds.has(course.id)
    return {
      id: String(course.id),
      name: course.courseName,
      value: degree,
      symbolSize: Math.max(48, Math.min(86, 48 + degree * 8)),
      itemStyle: {
        color: course.id === selectedId ? '#ff3151' : active ? '#ffb21c' : '#f3f4f6',
        borderColor: '#111',
        borderWidth: 1
      },
      label: {
        color: active ? '#111' : '#888'
      },
      emphasis: {
        scale: true
      }
    }
  })

  const links = relations.value.map(relation => {
    const route = relationRoute(relation)
    const option = relationTypeOptions.find(item => item.type === relation.relationType)
    const active = !selectedId || relation.courseId === selectedId || relation.relatedCourseId === selectedId
    return {
      source: String(route.source),
      target: String(route.target),
      value: relationTypeLabel(relation.relationType),
      lineStyle: {
        color: option?.color || '#111',
        width: active ? 3 : 1.4,
        opacity: active ? 0.9 : 0.16,
        curveness: relation.relationType === 'RELATED' ? 0.18 : 0.08,
        type: relation.relationType === 'RELATED' ? 'dashed' : 'solid'
      },
      label: {
        show: active,
        formatter: relationTypeLabel(relation.relationType)
      }
    }
  })

  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      formatter(params) {
        if (params.dataType === 'edge') {
          return params.data.value
        }
        const course = courseMap.value.get(Number(params.data.id))
        return `${course?.courseName || params.name}<br/>关系数：${params.data.value}`
      }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 12],
        force: {
          repulsion: 360,
          gravity: 0.08,
          edgeLength: [120, 220],
          friction: 0.55
        },
        label: {
          show: true,
          position: 'inside',
          fontSize: 12,
          fontWeight: 800,
          width: 96,
          overflow: 'truncate'
        },
        edgeLabel: {
          show: true,
          fontSize: 11,
          color: '#111',
          backgroundColor: 'rgba(255,255,255,0.78)',
          padding: [3, 5],
          borderRadius: 8
        },
        data: nodes,
        links
      }
    ]
  })
}

function resizeChart() {
  if (chart) {
    chart.resize()
  }
}

function degreeOf(courseId) {
  return relations.value.filter(relation => (
    relation.courseId === courseId || relation.relatedCourseId === courseId
  )).length
}

function relationRoute(relation) {
  if (relation.relationType === 'PREREQUISITE') {
    return { source: relation.relatedCourseId, target: relation.courseId }
  }
  return { source: relation.courseId, target: relation.relatedCourseId }
}

function routeStartLabel(relation) {
  const route = relationRoute(relation)
  return courseName(route.source)
}

function routeEndLabel(relation) {
  const route = relationRoute(relation)
  return courseName(route.target)
}

function relationTypeLabel(type) {
  return relationTypeOptions.find(option => option.type === type)?.label || '课程关系'
}

function courseName(courseId) {
  return courseMap.value.get(courseId)?.courseName || `课程 #${courseId}`
}

function courseCodeLabel(courseId) {
  const course = courseMap.value.get(courseId)
  return course?.courseCode || course?.semester || `#${courseId}`
}

function courseLabel(course) {
  return `${course.courseName} / ${course.courseCode || '未设置编号'}`
}
</script>

<style scoped>
:global(body) {
  margin: 0;
  color: #0d0d0d;
  background: #fff;
  font-family: Inter, "Helvetica Neue", Arial, "PingFang SC", "Microsoft YaHei", sans-serif;
}

button {
  color: inherit;
  font: inherit;
}

.course-map-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 78% 12%, rgba(255, 178, 28, 0.22), transparent 27%),
    radial-gradient(circle at 12% 82%, rgba(20, 203, 234, 0.17), transparent 30%),
    #fff;
}

.map-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;
  min-height: 96px;
  padding: 18px clamp(24px, 5vw, 72px);
  border-bottom: 1px solid #111;
  background: rgba(255, 255, 255, 0.93);
  backdrop-filter: blur(14px);
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.map-topbar h1 {
  margin: 8px 0 0;
  font-size: clamp(38px, 5vw, 76px);
  letter-spacing: -0.07em;
  line-height: 0.86;
}

.map-topbar h1 span {
  color: #ff3151;
}

.back-button,
.map-account button,
.relation-builder button,
.panel-heading button,
.graph-toolbar button,
.relation-map-card button,
.empty-state button {
  border: 1px solid #111;
  border-radius: 999px;
  background: #fff;
  font-weight: 850;
  cursor: pointer;
}

.back-button {
  padding: 12px 18px;
}

.map-account {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.map-account button {
  padding: 8px 12px;
}

.account-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #0de0c0;
}

.map-workspace {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 0;
  min-height: calc(100vh - 96px);
}

.map-sidebar {
  position: sticky;
  top: 96px;
  height: calc(100vh - 96px);
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow-y: auto;
  padding: 24px;
  border-right: 1px solid #111;
  background: rgba(255, 255, 255, 0.88);
}

.sidebar-card,
.relation-builder,
.course-list-panel,
.map-hero,
.graph-shell,
.relation-list-section {
  border: 1px solid #111;
  background: rgba(255, 255, 255, 0.94);
}

.identity-card {
  display: grid;
  gap: 8px;
  padding: 22px;
  box-shadow: 9px 9px 0 #0de0c0;
}

.identity-card strong {
  font-size: 64px;
  letter-spacing: -0.08em;
  line-height: 0.85;
}

.identity-card span {
  color: #666;
  line-height: 1.5;
}

.relation-builder {
  display: grid;
  gap: 15px;
  padding: 20px;
  box-shadow: 9px 9px 0 #ffb21c;
}

.relation-builder h2 {
  margin: 8px 0 0;
  font-size: 28px;
  letter-spacing: -0.05em;
}

.relation-builder label > span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

:deep(.relation-builder .el-select),
:deep(.relation-builder .el-input) {
  width: 100%;
}

:deep(.relation-builder .el-select__wrapper),
:deep(.relation-builder .el-input__wrapper) {
  min-height: 46px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.relation-builder button {
  min-height: 48px;
  background: #111;
  color: #fff;
}

.relation-builder button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.course-list-panel {
  min-height: 260px;
  overflow: hidden;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #111;
}

.panel-heading strong {
  font-size: 20px;
  letter-spacing: -0.035em;
}

.panel-heading button {
  padding: 7px 12px;
}

.course-node-list {
  display: grid;
  gap: 0;
  max-height: 360px;
  overflow-y: auto;
}

.course-node-list button {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 15px;
  border: 0;
  border-bottom: 1px solid #ddd;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.course-node-list button:hover,
.course-node-list button.active {
  background: #f8f6f1;
}

.course-node-list button.active {
  box-shadow: inset 5px 0 0 #ff3151;
}

.course-node-list span {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid #111;
  border-radius: 50%;
  background: #ffef5a;
  font-size: 12px;
  font-weight: 900;
}

.course-node-list strong,
.course-node-list small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-node-list small {
  grid-column: 2;
  color: #666;
  font-size: 11px;
}

.map-main {
  min-width: 0;
  display: grid;
  gap: 24px;
  padding: 32px clamp(26px, 4vw, 58px) 72px;
}

.map-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.62fr);
  gap: 32px;
  align-items: end;
  padding: clamp(26px, 4vw, 52px);
  box-shadow: 14px 14px 0 #14cbea;
}

.map-hero h2 {
  margin: 14px 0 18px;
  font-size: clamp(42px, 5vw, 82px);
  letter-spacing: -0.07em;
  line-height: 0.9;
}

.map-hero p:last-child {
  max-width: 760px;
  margin: 0;
  color: #3f4352;
  line-height: 1.75;
}

.map-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.map-stats article {
  min-height: 92px;
  padding: 15px;
  border: 1px solid #111;
  background: #fff;
}

.map-stats span,
.map-stats strong {
  display: block;
}

.map-stats span {
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.map-stats strong {
  margin-top: 9px;
  font-size: 30px;
  line-height: 1;
}

.graph-shell {
  position: relative;
  min-height: 620px;
  padding: 18px;
  background:
    linear-gradient(90deg, rgba(17, 17, 17, 0.04) 1px, transparent 1px),
    linear-gradient(rgba(17, 17, 17, 0.04) 1px, transparent 1px),
    rgba(255, 255, 255, 0.88);
  background-size: 38px 38px;
}

.graph-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.legend span {
  padding: 7px 11px;
  border: 1px solid #111;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
}

.legend-prerequisite {
  background: #ffe3e8;
}

.legend-related {
  background: #daf7ff;
}

.legend-follow-up {
  background: #dff8ed;
}

.graph-toolbar button {
  padding: 10px 16px;
}

.relation-chart {
  width: 100%;
  height: 560px;
}

.graph-empty,
.empty-card,
.empty-state {
  display: grid;
  place-content: center;
  color: #666;
  text-align: center;
}

.graph-empty {
  position: absolute;
  inset: 70px 18px 18px;
}

.relation-list-section {
  padding: 24px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: end;
  padding-bottom: 20px;
  border-bottom: 1px solid #111;
}

.section-heading h2 {
  margin: 8px 0 0;
  font-size: clamp(32px, 4vw, 58px);
  letter-spacing: -0.06em;
  line-height: 0.92;
}

.section-heading > span {
  font-size: 13px;
  font-weight: 900;
}

.relation-card-grid {
  min-height: 220px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.relation-map-card,
.empty-card,
.empty-state {
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
}

.relation-map-card {
  display: grid;
  gap: 14px;
  box-shadow: 8px 8px 0 #ffc0d0;
}

.relation-map-card-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.relation-map-card-top span {
  padding: 6px 10px;
  border: 1px solid #111;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 850;
}

.type-PREREQUISITE {
  background: #ffe3e8;
}

.type-RELATED {
  background: #daf7ff;
}

.type-FOLLOW_UP {
  background: #dff8ed;
}

.relation-map-card button {
  padding: 6px 11px;
  color: #ff3151;
}

.relation-route {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.relation-route strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 20px;
  letter-spacing: -0.04em;
}

.relation-route i {
  color: #ff3151;
  font-style: normal;
  font-weight: 900;
}

.relation-map-card p {
  margin: 0;
  color: #4b5563;
  line-height: 1.65;
}

.relation-map-card small {
  color: #777;
  font-size: 11px;
}

.empty-card,
.empty-state {
  min-height: 220px;
}

.empty-card strong,
.empty-state strong {
  color: #111;
  font-size: 22px;
}

.empty-state {
  max-width: 560px;
  margin: 80px auto;
}

.empty-state button {
  justify-self: center;
  padding: 12px 20px;
  background: #ffb21c;
}

@media (max-width: 1160px) {
  .map-workspace,
  .map-hero,
  .map-topbar {
    grid-template-columns: 1fr;
  }

  .map-sidebar {
    position: static;
    height: auto;
    border-right: 0;
    border-bottom: 1px solid #111;
  }
}

@media (max-width: 720px) {
  .map-stats,
  .relation-card-grid {
    grid-template-columns: 1fr;
  }

  .relation-chart {
    height: 460px;
  }
}
</style>
