<template>
  <AuthPanel v-if="!currentUser" />

  <main v-else class="workspace">
    <DashboardTopbar
      :active-section="activeSection"
      @activate-section="activateSection"
    />
    <DashboardSearchShell
      :selected-course="selectedCourse"
      :search-loading="searchLoading"
      :course-switching="courseSwitching"
    />

    <div class="workspace-layout">
      <CourseSidebar
        :course-loading="courseLoading"
        :courses="courses"
        :selected-course="selectedCourse"
        :course-switching="courseSwitching"
        @select-course="selectCourse"
      />

      <section class="course-content" :class="{ 'course-content-switching': courseSwitching }">
        <template v-if="selectedCourse">
          <SectionDots
            :active-section="activeSection"
            @activate-section="activateSection"
          />
          <OverviewSection :active="activeSection === 'overview'" />
          <ChapterSection :active="activeSection === 'chapters'" />
          <MaterialSection :active="activeSection === 'materials'" />
          <SearchSection :active="activeSection === 'search'" />
          <KnowledgeSection :active="activeSection === 'knowledge'" />
          <GapSection :active="activeSection === 'gaps'" />
          <TeacherSection :active="activeSection === 'teacher'" />

          <ReviewSection
            :active="activeSection === 'review'"
            :profile-form="reviewProfileForm"
            :mock-exam-form="mockExamForm"
            :teacher-profiles="teacherProfiles"
            :difficulty-options="reviewDifficultyOptions"
            :output-options="reviewOutputOptions"
            :active-output-type="activeReviewOutputType"
            :custom-requirement="activeReviewCustomRequirement"
            :generating="isSelectedReviewGenerating"
            :active-result="activeReviewResult"
            :profiles="reviewProfiles"
            :profile-loading="reviewProfileLoading"
            :profile-saving="reviewProfileSaving"
            :editing-profile-id="editingReviewProfileId"
            :output-label="reviewOutputLabel"
            :difficulty-label="reviewDifficultyLabel"
            :render-markdown="renderMarkdown"
            @save-profile="saveReviewProfile"
            @reset-profile="resetReviewProfileForm"
            @select-output="selectReviewOutput"
            @generate="generateSelectedReviewAsset"
            @update:custom-requirement="activeReviewCustomRequirement = $event"
            @download-result="downloadReviewResult"
            @edit-profile="editReviewProfile"
            @remove-profile="removeReviewProfile"
          />

          <ExamSection :active="activeSection === 'exam'" />
          <ExportSection :active="activeSection === 'export'" />
        </template>

        <NoCourseSection v-else />
      </section>
    </div>
  </main>

  <DashboardDialogs />
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthPanel from '../../components/dashboard/AuthPanel.vue'
import CourseSidebar from '../../components/dashboard/CourseSidebar.vue'
import DashboardDialogs from '../../components/dashboard/DashboardDialogs.vue'
import DashboardSearchShell from '../../components/dashboard/DashboardSearchShell.vue'
import DashboardTopbar from '../../components/dashboard/DashboardTopbar.vue'
import ExamSection from '../../components/dashboard/ExamSection.vue'
import ExportSection from '../../components/dashboard/ExportSection.vue'
import GapSection from '../../components/dashboard/GapSection.vue'
import KnowledgeSection from '../../components/dashboard/KnowledgeSection.vue'
import MaterialSection from '../../components/dashboard/MaterialSection.vue'
import NoCourseSection from '../../components/dashboard/NoCourseSection.vue'
import OverviewSection from '../../components/dashboard/OverviewSection.vue'
import ChapterSection from '../../components/dashboard/ChapterSection.vue'
import ReviewSection from '../../components/dashboard/ReviewSection.vue'
import SearchSection from '../../components/dashboard/SearchSection.vue'
import SectionDots from '../../components/dashboard/SectionDots.vue'
import TeacherSection from '../../components/dashboard/TeacherSection.vue'
import { provideDashboardContext } from '../../components/dashboard/DashboardContext'
import { dashboardSectionPath, normalizeDashboardSection } from './dashboardPages'
import { login, register } from '../../api/auth'
import {
  createChapter,
  createCourse,
  createCourseRelation,
  deleteChapter,
  deleteCourse,
  deleteCourseRelation,
  getCourseStats,
  listChapters,
  listCourses,
  listCourseRelations,
  updateChapter,
  updateCourse
} from '../../api/course'
import {
  deleteMaterial,
  generateMaterialSummary,
  listSimilarMaterials,
  listMaterials,
  listTextChunks,
  materialFileUrl,
  parseMaterial,
  previewMaterialContent,
  updateMaterial,
  uploadMaterialsBatch
} from '../../api/material'
import { listSearchRecords, searchMaterials } from '../../api/search'
import {
  deleteKnowledgeItem,
  generateKnowledgeItems,
  updateKnowledgeMastery,
  listCourseTags,
  listKnowledgeItems,
  listMaterialTags,
  previewMaterialTags,
  replaceMaterialTags
} from '../../api/knowledge'
import {
  createExport,
  exportDownloadUrl,
  listExportRecords,
  listExportTemplates,
  markExportRecommended,
  previewExport
} from '../../api/export'
import {
  createKnowledgeGapReport,
  getKnowledgeRemediationPath,
  listKnowledgeGapItems,
  listKnowledgeGapReports,
  listPrerequisiteGapHints
} from '../../api/gap'
import {
  analyzeTeacherProfile,
  listTeacherProfileEvidence,
  listTeacherProfiles,
  recalculateTeacherProfileConfidence,
  reanalyzeTeacherProfile,
  updateTeacherProfile
} from '../../api/teacher'
import {
  generateMockExam,
  mockExamDownloadUrl
} from '../../api/mockExam'
import {
  generateSprintOutline,
  sprintOutlineDownloadUrl
} from '../../api/sprintOutline'
import {
  createReviewProfile,
  deleteReviewProfile,
  generateReviewAsset,
  listReviewProfiles,
  reviewAssetDownloadUrl,
  updateReviewProfile
} from '../../api/review'
import {
  createAiProvider,
  deleteAiProvider,
  getAiStatus,
  listAiGenerationTasks,
  listAiProviders,
  updateAiProvider
} from '../../api/ai'

const router = useRouter()
const route = useRoute()
const currentUser = ref(null)
const authMode = ref('login')
const authLoading = ref(false)
const courseLoading = ref(false)
const courseStatsLoading = ref(false)
const chapterLoading = ref(false)
const materialLoading = ref(false)
const searchLoading = ref(false)
const searchRecordLoading = ref(false)
const searchHasRun = ref(false)
const relationLoading = ref(false)
const relationSaving = ref(false)
const knowledgeLoading = ref(false)
const knowledgeGenerating = ref(false)
const gapLoading = ref(false)
const gapGenerating = ref(false)
const gapItemLoading = ref(false)
const prerequisiteGapLoading = ref(false)
const remediationPathLoading = ref(false)
const teacherProfileLoading = ref(false)
const teacherProfileAnalyzing = ref(false)
const teacherEvidenceLoading = ref(false)
const teacherProfileEditing = ref(false)
const teacherProfileSaving = ref(false)
const teacherConfidenceScoring = ref(false)
const teacherProfileReanalyzing = ref(false)
const mockExamGenerating = ref(false)
const sprintOutlineGenerating = ref(false)
const reviewAssetGenerating = ref(false)
const reviewProfileLoading = ref(false)
const reviewProfileSaving = ref(false)
const aiProviderLoading = ref(false)
const aiProviderSaving = ref(false)
const aiTaskLoading = ref(false)
const tagSaving = ref(false)
const tagPreviewLoading = ref(false)
const exportLoading = ref(false)
const exportCreating = ref(false)
const exportPreviewLoading = ref(false)
const courseSaving = ref(false)
const chapterSaving = ref(false)
const materialSaving = ref(false)
const parsingMaterialId = ref(null)
const summaryGeneratingIds = ref([])
const textChunkLoading = ref(false)
const courseDialogVisible = ref(false)
const chapterDialogVisible = ref(false)
const materialDialogVisible = ref(false)
const textPreviewVisible = ref(false)
const similarDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const deleteLoading = ref(false)
const deleteTarget = ref(null)
const editingCourseId = ref(null)
const editingChapterId = ref(null)
const editingMaterialId = ref(null)
const courses = ref([])
const chapters = ref([])
const materials = ref([])
const courseRelations = ref([])
const courseStats = ref(null)
const searchResults = ref([])
const searchRecords = ref([])
const knowledgeItems = ref([])
const gapReports = ref([])
const gapItems = ref([])
const prerequisiteGapHints = ref([])
const remediationPath = ref(null)
const teacherProfiles = ref([])
const teacherProfileEvidence = ref([])
const mockExamResult = ref(null)
const sprintOutlineResult = ref(null)
const reviewAssetResult = ref(null)
const reviewProfiles = ref([])
const aiProviders = ref([])
const aiGenerationTasks = ref([])
const aiDefaultStatus = ref(null)
const courseTags = ref([])
const exportTemplates = ref([])
const exportRecords = ref([])
const exportPreview = ref(null)
const masterySavingIds = ref([])
const selectedMaterialTags = ref([])
const previewedMaterialTags = ref([])
const knowledgeFilterType = ref(null)
const selectedCourse = ref(null)
const courseSwitching = ref(false)
const selectedGapReport = ref(null)
const selectedTeacherProfile = ref(null)
const editingReviewProfileId = ref(null)
const editingAiProviderId = ref(null)
const uploadFileList = ref([])
const uploadDrafts = ref([])
const materialUploadRef = ref(null)
const previewMaterial = ref(null)
const similarSourceMaterial = ref(null)
const similarMaterialResults = ref([])
const textChunks = ref([])
const materialPreviewContent = ref('')
const materialPreviewMode = ref('text')
const activeSection = ref(normalizeDashboardSection(route.params.section))
const examPreferredMaterialId = ref(null)
const activeReviewOutputType = ref('REVIEW_NOTE')
const quickSearchKeyword = ref('')
let courseSwitchTimer = null

const pageSections = [
  { id: 'overview', label: '概览', title: '课程概览' },
  { id: 'chapters', label: '章节', title: '章节路径' },
  { id: 'materials', label: '资料', title: '课程资料' },
  { id: 'search', label: '检索', title: '课程检索' },
  { id: 'knowledge', label: '知识', title: '知识条目' },
  { id: 'gaps', label: '缺口', title: '知识缺口' },
  { id: 'teacher', label: '画像', title: '教师画像' },
  { id: 'review', label: '复习', title: '复习配置' },
  { id: 'exam', label: '真题', title: '真题映射' },
  { id: 'export', label: '导出', title: '知识包导出' }
]

const primaryPageSections = computed(() => pageSections.filter(section => section.id !== 'search'))

const materialTypeOptions = [
  { type: 'SLIDE', label: '课件', icon: 'PPT' },
  { type: 'LAB_REPORT', label: '实验报告', icon: 'LAB' },
  { type: 'EXAM', label: '往年真题', icon: 'EXAM' },
  { type: 'NOTE', label: '复习笔记', icon: 'NOTE' },
  { type: 'CODE', label: '代码样例', icon: 'CODE' },
  { type: 'OTHER', label: '其他资料', icon: 'FILE' }
]

const materialGroups = computed(() => materialTypeOptions.map(group => ({
  ...group,
  items: materials.value.filter(material => material.materialType === group.type)
})))

const aiTaskStats = computed(() => ({
  total: aiGenerationTasks.value.length,
  running: aiGenerationTasks.value.filter(task => task.status === 'RUNNING' || task.status === 'PENDING').length,
  failed: aiGenerationTasks.value.filter(task => task.status === 'FAILED').length
}))

const overviewStats = computed(() => ({
  materialCount: courseStats.value?.materialCount ?? materials.value.length,
  parsedMaterialCount: courseStats.value?.parsedMaterialCount
    ?? materials.value.filter(item => item.parsedChunkCount > 0).length,
  knowledgeItemCount: courseStats.value?.knowledgeItemCount ?? knowledgeItems.value.length,
  examQuestionCount: courseStats.value?.examQuestionCount ?? 0,
  examMappingCount: courseStats.value?.examMappingCount ?? 0,
  exportCount: courseStats.value?.exportCount ?? exportRecords.value.length
}))

const materialTypeDistribution = computed(() => {
  const stats = courseStats.value?.materialTypeStats || []
  return materialTypeOptions.map(option => {
    const match = stats.find(item => item.materialType === option.type)
    const count = match?.count || 0
    const total = overviewStats.value.materialCount || 0
    return {
      ...option,
      count,
      percent: total > 0 ? Math.round((count / total) * 100) : 0
    }
  })
})

const parsedMaterials = computed(() => materials.value.filter(
  material => material.parsedChunkCount > 0
))

const materialReaderUrl = computed(() => (
  previewMaterial.value && currentUser.value
    ? materialFileUrl(previewMaterial.value.id, currentUser.value.id, 'inline')
    : ''
))

const canDownloadPreviewMaterial = computed(() => Boolean(previewMaterial.value && currentUser.value))

const materialReaderFooter = computed(() => {
  if (!previewMaterial.value) return '未选择资料'
  const type = (previewMaterial.value.fileType || 'FILE').toUpperCase()
  if (materialPreviewMode.value === 'chunks') return `共 ${textChunks.value.length} 个文本块`
  if (materialPreviewMode.value === 'markdown') return `${type} / Markdown 预览`
  if (materialPreviewMode.value === 'text') return `${type} / 文本预览`
  if (materialPreviewMode.value === 'pdf') return 'PDF 原文件预览'
  return `${type} / 原文件`
})

const knowledgeTypeOptions = [
  { type: 'DEFINITION', label: '定义' },
  { type: 'KEY_POINT', label: '重点' },
  { type: 'FORMULA', label: '公式' },
  { type: 'METHOD', label: '方法' },
  { type: 'EXAMPLE', label: '例子' },
  { type: 'WARNING', label: '易错点' }
]

const masteryStatusOptions = [
  { status: 'UNKNOWN', label: '未评估' },
  { status: 'LEARNING', label: '学习中' },
  { status: 'MASTERED', label: '已掌握' }
]

const reviewDifficultyOptions = [
  { value: 'EASY', label: '基础' },
  { value: 'MEDIUM', label: '标准' },
  { value: 'MEDIUM_HARD', label: '偏难' },
  { value: 'HARD', label: '冲刺' }
]

const reviewOutputOptions = [
  { value: 'REVIEW_NOTE', label: '复习笔记', hint: '按知识点整理可直接背诵的笔记', accent: '#ffb21c' },
  { value: 'OUTLINE', label: '复习提纲', hint: '生成章节化复习路径和优先级', accent: '#14cbea' },
  { value: 'FLASHCARDS', label: '记忆卡片', hint: '整理成问答卡片，适合快速自测', accent: '#0de0c0' },
  { value: 'MOCK_EXAM', label: '模拟题', hint: '按教师画像和考点生成练习题', accent: '#ff3151' },
  { value: 'CHECKLIST', label: '检查清单', hint: '输出复习任务勾选清单', accent: '#ad93ff' }
]

const relationTypeOptions = [
  { type: 'PREREQUISITE', label: '前置课程', exportField: 'includePrerequisiteCourses' },
  { type: 'RELATED', label: '关联课程', exportField: 'includeRelatedCourses' },
  { type: 'FOLLOW_UP', label: '后续课程', exportField: 'includeFollowUpCourses' }
]

const relationCandidateCourses = computed(() => {
  const relatedIds = new Set(courseRelations.value.map(item => item.relatedCourseId))
  return courses.value.filter(course => (
    selectedCourse.value
    && course.id !== selectedCourse.value.id
    && !relatedIds.has(course.id)
  ))
})

const relationGroups = computed(() => relationTypeOptions.map(option => ({
  ...option,
  items: courseRelations.value.filter(item => item.relationType === option.type)
})))

const exportRelationOptions = computed(() => relationTypeOptions.map(option => ({
  ...option,
  count: courseRelations.value.filter(item => item.relationType === option.type).length
})))

const activeReviewCustomRequirement = computed({
  get() {
    if (activeReviewOutputType.value === 'MOCK_EXAM') {
      return mockExamForm.customRequirement
    }
    if (activeReviewOutputType.value === 'OUTLINE') {
      return sprintOutlineForm.customRequirement
    }
    return reviewProfileForm.customRequirement
  },
  set(value) {
    if (activeReviewOutputType.value === 'MOCK_EXAM') {
      mockExamForm.customRequirement = value
    } else if (activeReviewOutputType.value === 'OUTLINE') {
      sprintOutlineForm.customRequirement = value
    } else {
      reviewProfileForm.customRequirement = value
    }
  }
})

const isSelectedReviewGenerating = computed(() => (
  activeReviewOutputType.value === 'MOCK_EXAM'
    ? mockExamGenerating.value
    : reviewAssetGenerating.value
))

const activeReviewResult = computed(() => {
  if (activeReviewOutputType.value === 'MOCK_EXAM') {
    return normalizeReviewResult(mockExamResult.value, 'MOCK_EXAM')
  }
  if (reviewAssetResult.value?.outputType !== activeReviewOutputType.value) {
    return null
  }
  return normalizeReviewResult(reviewAssetResult.value, activeReviewOutputType.value)
})

const gapStats = computed(() => ({
  total: gapItems.value.length,
  severe: gapItems.value.filter(item => item.severityLevel >= 4).length,
  prerequisite: gapItems.value.filter(item => item.relatedCourseRelationId).length
}))

const prerequisiteRelationCount = computed(() => (
  courseRelations.value.filter(item => item.relationType === 'PREREQUISITE').length
))

const teacherProfileMaterials = computed(() => materials.value.filter(material => (
  material.parsedChunkCount > 0 || material.materialType === 'EXAM'
)))

const deleteDialogTitle = computed(() => {
  if (deleteTarget.value?.type === 'course') return '删除这门课程'
  if (deleteTarget.value?.type === 'chapter') return '删除这个章节'
  if (deleteTarget.value?.type === 'knowledge') return '删除这条知识'
  return '删除这份资料'
})

const deleteDialogDescription = computed(() => {
  if (deleteTarget.value?.type === 'course') {
    return '课程内的章节、资料、解析文本和相关数据都会一并删除。'
  }
  if (deleteTarget.value?.type === 'chapter') {
    return '章节会被删除，但其中的资料将保留并变为未关联章节。'
  }
  if (deleteTarget.value?.type === 'knowledge') {
    return '该知识条目会从课程知识库中移除，原始资料和解析文本不会受影响。'
  }
  return '原始文件、解析文本和相关数据都会一并删除。'
})

const authForm = reactive({
  username: '',
  password: '',
  email: ''
})

const courseForm = reactive({
  courseName: '',
  courseCode: '',
  semester: '',
  description: ''
})

const chapterForm = reactive({
  chapterNo: '',
  chapterTitle: '',
  sortOrder: 0
})

const materialForm = reactive({
  title: '',
  materialType: 'SLIDE',
  chapterId: null,
  year: new Date().getFullYear(),
  isKey: false,
  summary: ''
})

const searchForm = reactive({
  keyword: '',
  chapterId: null,
  materialType: null,
  isKey: false
})

const relationForm = reactive({
  relatedCourseId: null,
  relationType: 'PREREQUISITE',
  reason: ''
})

const knowledgeForm = reactive({
  materialId: null,
  maxItems: 12,
  replaceExisting: false
})

const gapForm = reactive({
  reportName: '',
  includePrerequisites: true
})

const teacherProfileForm = reactive({
  teacherName: '',
  materialIds: []
})

const mockExamForm = reactive({
  questionCount: 8,
  difficultyLevel: 'MEDIUM',
  customRequirement: ''
})

const sprintOutlineForm = reactive({
  days: 7,
  customRequirement: ''
})

const teacherEditForm = reactive({
  teacherName: '',
  confidenceScore: 50,
  examStyle: '',
  questionPreference: '',
  gradingPreference: '',
  focusTopics: '',
  avoidTopics: '',
  sourceSummary: ''
})

const reviewProfileForm = reactive({
  profileName: '',
  target: '',
  difficultyLevel: 'MEDIUM',
  outputType: 'REVIEW_NOTE',
  includePrerequisites: true,
  teacherProfileId: null,
  customRequirement: ''
})

const aiProviderForm = reactive({
  providerName: 'DeepSeek',
  baseUrl: 'https://api.deepseek.com',
  modelName: 'deepseek-v4-flash',
  apiKeyAlias: 'DEEPSEEK_API_KEY',
  enabled: true
})

const exportForm = reactive({
  exportName: '',
  templateId: null,
  chapterIds: [],
  materialTypes: [],
  onlyKeyMaterials: false,
  includeExamStats: true,
  includePrerequisiteCourses: false,
  includeRelatedCourses: false,
  includeFollowUpCourses: false
})

provideDashboardContext({
  router,
  currentUser,
  authMode,
  authLoading,
  courseLoading,
  courseStatsLoading,
  chapterLoading,
  materialLoading,
  searchLoading,
  searchRecordLoading,
  searchHasRun,
  relationLoading,
  relationSaving,
  knowledgeLoading,
  knowledgeGenerating,
  gapLoading,
  gapGenerating,
  gapItemLoading,
  prerequisiteGapLoading,
  remediationPathLoading,
  teacherProfileLoading,
  teacherProfileAnalyzing,
  teacherEvidenceLoading,
  teacherProfileEditing,
  teacherProfileSaving,
  teacherConfidenceScoring,
  teacherProfileReanalyzing,
  mockExamGenerating,
  sprintOutlineGenerating,
  reviewAssetGenerating,
  reviewProfileLoading,
  reviewProfileSaving,
  aiProviderLoading,
  aiProviderSaving,
  aiTaskLoading,
  tagSaving,
  tagPreviewLoading,
  exportLoading,
  exportCreating,
  exportPreviewLoading,
  courseSaving,
  chapterSaving,
  materialSaving,
  parsingMaterialId,
  summaryGeneratingIds,
  textChunkLoading,
  courseDialogVisible,
  chapterDialogVisible,
  materialDialogVisible,
  textPreviewVisible,
  similarDialogVisible,
  deleteDialogVisible,
  deleteLoading,
  deleteTarget,
  editingCourseId,
  editingChapterId,
  editingMaterialId,
  courses,
  chapters,
  materials,
  courseRelations,
  courseStats,
  searchResults,
  searchRecords,
  knowledgeItems,
  gapReports,
  gapItems,
  prerequisiteGapHints,
  remediationPath,
  teacherProfiles,
  teacherProfileEvidence,
  mockExamResult,
  sprintOutlineResult,
  reviewAssetResult,
  reviewProfiles,
  aiProviders,
  aiGenerationTasks,
  aiDefaultStatus,
  courseTags,
  exportTemplates,
  exportRecords,
  exportPreview,
  masterySavingIds,
  selectedMaterialTags,
  previewedMaterialTags,
  knowledgeFilterType,
  selectedCourse,
  selectedGapReport,
  selectedTeacherProfile,
  editingReviewProfileId,
  editingAiProviderId,
  uploadFileList,
  uploadDrafts,
  materialUploadRef,
  previewMaterial,
  similarSourceMaterial,
  similarMaterialResults,
  textChunks,
  materialPreviewContent,
  materialPreviewMode,
  activeSection,
  courseSwitching,
  examPreferredMaterialId,
  activeReviewOutputType,
  quickSearchKeyword,
  pageSections,
  primaryPageSections,
  materialTypeOptions,
  materialGroups,
  aiTaskStats,
  overviewStats,
  materialTypeDistribution,
  parsedMaterials,
  materialReaderUrl,
  canDownloadPreviewMaterial,
  materialReaderFooter,
  knowledgeTypeOptions,
  masteryStatusOptions,
  reviewDifficultyOptions,
  reviewOutputOptions,
  relationTypeOptions,
  relationCandidateCourses,
  relationGroups,
  exportRelationOptions,
  activeReviewCustomRequirement,
  isSelectedReviewGenerating,
  activeReviewResult,
  gapStats,
  prerequisiteRelationCount,
  teacherProfileMaterials,
  deleteDialogTitle,
  deleteDialogDescription,
  authForm,
  courseForm,
  chapterForm,
  materialForm,
  searchForm,
  relationForm,
  knowledgeForm,
  gapForm,
  teacherProfileForm,
  mockExamForm,
  sprintOutlineForm,
  teacherEditForm,
  reviewProfileForm,
  aiProviderForm,
  exportForm,
  scrollToSection,
  openDetailedSearch,
  submitAuth,
  loadCourses,
  selectCourse,
  activateSection,
  loadCourseRelations,
  loadCourseStats,
  loadSearchRecords,
  saveRelation,
  removeRelation,
  loadExportData,
  loadGapReports,
  loadPrerequisiteGapHints,
  generateGapReport,
  selectGapReport,
  loadRemediationPath,
  loadTeacherProfiles,
  runTeacherProfileAnalysis,
  selectTeacherProfile,
  recalculateTeacherConfidence,
  runTeacherProfileReanalysis,
  generateMockExamFromProfile,
  downloadMockExam,
  generateSprintOutlineFromProfile,
  downloadSprintOutline,
  selectReviewOutput,
  generateSelectedReviewAsset,
  generateReviewAssetFromProfile,
  downloadReviewResult,
  loadReviewProfiles,
  saveReviewProfile,
  editReviewProfile,
  removeReviewProfile,
  loadAiProviders,
  loadAiGenerationTasks,
  saveAiProvider,
  editAiProvider,
  removeAiProvider,
  openTeacherProfileEditor,
  cancelTeacherProfileEdit,
  saveTeacherProfileEdit,
  runExport,
  runExportPreview,
  buildExportPayload,
  downloadExport,
  setRecommendedExport,
  formatExportTime,
  loadKnowledgeItems,
  loadCourseTags,
  loadSelectedMaterialTags,
  saveSelectedMaterialTags,
  previewTags,
  togglePreviewTag,
  applyPreviewTags,
  generateKnowledge,
  saveKnowledgeMastery,
  isMasterySaving,
  formatReviewTime,
  formatSearchTime,
  searchTypeLabel,
  knowledgeTypeLabel,
  runSearch,
  runQuickSearch,
  resetSearch,
  rerunSearchRecord,
  resetRelationForm,
  resetGapForm,
  clearGapState,
  resetTeacherProfileForm,
  resetMockExamForm,
  resetSprintOutlineForm,
  clearTeacherProfileState,
  resetReviewProfileForm,
  clearReviewProfileState,
  resetAiProviderForm,
  clearAiProviderState,
  resetExportForm,
  hasCourseRelationsOfType,
  materialTypeLabel,
  masteryStatusLabel,
  gapTypeLabel,
  relationTypeLabel,
  formatGapTime,
  teacherStatusLabel,
  evidenceTypeLabel,
  reviewDifficultyLabel,
  reviewOutputLabel,
  normalizeReviewResult,
  renderMarkdown,
  renderMarkdownTable,
  inlineMarkdown,
  escapeHtml,
  aiTaskTypeLabel,
  aiTaskStatusLabel,
  formatAiTaskTime,
  matchSourceLabel,
  searchResultMaterial,
  editSearchResult,
  previewSearchResult,
  openKnowledgeResult,
  saveMaterial,
  handleFileChange,
  handleFileRemove,
  syncUploadDrafts,
  createUploadDraft,
  runMaterialParse,
  isSummaryGenerating,
  runMaterialSummary,
  checkSimilarMaterials,
  openSimilarMaterial,
  addGeneratedMaterial,
  materialFileType,
  canParseMaterial,
  openMaterialReader,
  downloadPreviewMaterial,
  showParsedText,
  openExamWorkbench,
  handleExamMaterialParsed,
  saveCourse,
  saveChapter,
  confirmCourseDeletion,
  confirmChapterDeletion,
  confirmMaterialDeletion,
  confirmKnowledgeDeletion,
  executeDeletion,
  removeKnowledgeItem,
  removeCourse,
  removeChapter,
  removeMaterial,
  resetDeleteTarget,
  logout,
  openCourseCreator,
  openCourseEditor,
  openChapterCreator,
  openChapterEditor,
  openMaterialUploader,
  openMaterialEditor,
  replaceItem,
  chapterName,
  resetCourseForm,
  resetChapterForm,
  resetMaterialForm,
  formatFileSize
})


onMounted(() => {
  const savedUser = localStorage.getItem('ai4note-user')
  if (savedUser) {
    currentUser.value = JSON.parse(savedUser)
    loadCourses()
  }
})

watch(
  () => route.params.section,
  (section) => {
    activeSection.value = normalizeDashboardSection(section)
  }
)

function scrollToSection(sectionId) {
  if (pageSections.some(section => section.id === sectionId)) {
    activateSection(sectionId)
  }
}

function activateSection(sectionId, syncRoute = true) {
  const normalizedSection = normalizeDashboardSection(sectionId)
  activeSection.value = normalizedSection
  if (syncRoute && route.name !== 'dashboard-section') {
    router.push(dashboardSectionPath(normalizedSection))
  } else if (syncRoute && route.params.section !== normalizedSection) {
    router.replace(dashboardSectionPath(normalizedSection))
  }
}

function openDetailedSearch() {
  if (!selectedCourse.value) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (quickSearchKeyword.value.trim() && quickSearchKeyword.value.trim() !== searchForm.keyword) {
    searchForm.keyword = quickSearchKeyword.value.trim()
  }
  activateSection('search')
}

async function submitAuth() {
  if (!authForm.username.trim() || !authForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  authLoading.value = true
  try {
    const action = authMode.value === 'login' ? login : register
    const user = await action({
      username: authForm.username.trim(),
      password: authForm.password,
      email: authMode.value === 'register' ? authForm.email.trim() || null : undefined
    })
    currentUser.value = user
    localStorage.setItem('ai4note-user', JSON.stringify(user))
    ElMessage.success(authMode.value === 'login' ? '登录成功' : '账号创建成功')
    await loadCourses()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    authLoading.value = false
  }
}

async function loadCourses() {
  courseLoading.value = true
  try {
    courses.value = await listCourses(currentUser.value.id)
    if (courses.value.length > 0) {
      await selectCourse(courses.value[0])
    } else {
      selectedCourse.value = null
      chapters.value = []
      materials.value = []
      courseRelations.value = []
      courseStats.value = null
      searchRecords.value = []
      clearGapState()
      clearTeacherProfileState()
      clearReviewProfileState()
      clearAiProviderState()
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseLoading.value = false
  }
}

async function selectCourse(course) {
  if (selectedCourse.value?.id === course.id) return
  const shouldAnimateSwitch = Boolean(selectedCourse.value?.id)
  if (shouldAnimateSwitch) {
    startCourseSwitchAnimation()
  }
  selectedCourse.value = course
  activateSection(route.params.section || activeSection.value, false)
  examPreferredMaterialId.value = null
  resetRelationForm()
  resetGapForm()
  resetTeacherProfileForm()
  resetReviewProfileForm()
  resetAiProviderForm()
  resetSearch()
  knowledgeForm.materialId = null
  selectedMaterialTags.value = []
  previewedMaterialTags.value = []
  knowledgeFilterType.value = null
  chapterLoading.value = true
  materialLoading.value = true
  try {
    const [chapterData, materialData, relationData] = await Promise.all([
      listChapters(course.id, currentUser.value.id),
      listMaterials(course.id, currentUser.value.id),
      listCourseRelations(course.id, currentUser.value.id)
    ])
    chapters.value = chapterData
    materials.value = materialData
    courseRelations.value = relationData
    resetExportForm()
    await Promise.all([
      loadKnowledgeItems(),
      loadCourseStats(),
      loadCourseTags(),
      loadSearchRecords(),
      loadExportData(),
      loadGapReports(),
      loadPrerequisiteGapHints(),
      loadTeacherProfiles(),
      loadReviewProfiles(),
      loadAiProviders(),
      loadAiGenerationTasks()
    ])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    chapterLoading.value = false
    materialLoading.value = false
    if (shouldAnimateSwitch) {
      finishCourseSwitchAnimation()
    }
  }
}

function startCourseSwitchAnimation() {
  if (courseSwitchTimer) {
    clearTimeout(courseSwitchTimer)
  }
  courseSwitching.value = true
}

function finishCourseSwitchAnimation() {
  if (courseSwitchTimer) {
    clearTimeout(courseSwitchTimer)
  }
  courseSwitchTimer = setTimeout(() => {
    courseSwitching.value = false
    courseSwitchTimer = null
  }, 760)
}

async function loadCourseRelations() {
  if (!selectedCourse.value) return
  relationLoading.value = true
  try {
    courseRelations.value = await listCourseRelations(selectedCourse.value.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    relationLoading.value = false
  }
}

async function loadCourseStats() {
  if (!selectedCourse.value || !currentUser.value) return
  courseStatsLoading.value = true
  try {
    courseStats.value = await getCourseStats(selectedCourse.value.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseStatsLoading.value = false
  }
}

async function loadSearchRecords() {
  if (!selectedCourse.value || !currentUser.value) return
  searchRecordLoading.value = true
  try {
    searchRecords.value = await listSearchRecords(currentUser.value.id, selectedCourse.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    searchRecordLoading.value = false
  }
}

async function saveRelation() {
  if (!relationForm.relatedCourseId) {
    ElMessage.warning('请选择关联课程')
    return
  }
  relationSaving.value = true
  try {
    const relation = await createCourseRelation(
      selectedCourse.value.id,
      currentUser.value.id,
      {
        relatedCourseId: relationForm.relatedCourseId,
        relationType: relationForm.relationType,
        reason: relationForm.reason.trim() || null,
        sortOrder: courseRelations.value.length
      }
    )
    courseRelations.value.push(relation)
    await loadPrerequisiteGapHints()
    resetRelationForm()
    ElMessage.success('课程关系已添加')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    relationSaving.value = false
  }
}

async function removeRelation(relation) {
  try {
    await deleteCourseRelation(relation.id, currentUser.value.id)
    courseRelations.value = courseRelations.value.filter(item => item.id !== relation.id)
    await loadPrerequisiteGapHints()
    ElMessage.success('课程关系已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadExportData() {
  if (!selectedCourse.value || !currentUser.value) return
  exportLoading.value = true
  try {
    const [templates, records] = await Promise.all([
      listExportTemplates(),
      listExportRecords(currentUser.value.id, selectedCourse.value.id)
    ])
    exportTemplates.value = templates
    exportRecords.value = records
    if (!exportForm.templateId && templates.length > 0) {
      exportForm.templateId = templates[0].id
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exportLoading.value = false
  }
}

async function loadGapReports() {
  if (!selectedCourse.value || !currentUser.value) return
  gapLoading.value = true
  try {
    gapReports.value = await listKnowledgeGapReports(selectedCourse.value.id, currentUser.value.id)
    if (gapReports.value.length > 0) {
      await selectGapReport(gapReports.value[0])
    } else {
      selectedGapReport.value = null
      gapItems.value = []
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    gapLoading.value = false
  }
}

async function loadPrerequisiteGapHints() {
  if (!selectedCourse.value || !currentUser.value) return
  prerequisiteGapLoading.value = true
  try {
    prerequisiteGapHints.value = await listPrerequisiteGapHints(
      selectedCourse.value.id,
      currentUser.value.id
    )
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    prerequisiteGapLoading.value = false
  }
}

async function generateGapReport() {
  if (!selectedCourse.value || !currentUser.value) return
  gapGenerating.value = true
  try {
    const report = await createKnowledgeGapReport(selectedCourse.value.id, {
      userId: currentUser.value.id,
      reportName: gapForm.reportName.trim() || null,
      includePrerequisites: gapForm.includePrerequisites
    })
    gapReports.value = [report, ...gapReports.value.filter(item => item.id !== report.id)]
    await selectGapReport(report)
    resetGapForm()
    ElMessage.success('知识缺口报告已生成')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    gapGenerating.value = false
  }
}

async function selectGapReport(report) {
  selectedGapReport.value = report
  remediationPath.value = null
  gapItemLoading.value = true
  try {
    gapItems.value = await listKnowledgeGapItems(report.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    gapItemLoading.value = false
  }
}

async function loadRemediationPath() {
  if (!selectedGapReport.value || !currentUser.value) return
  remediationPathLoading.value = true
  try {
    remediationPath.value = await getKnowledgeRemediationPath(
      selectedGapReport.value.id,
      currentUser.value.id
    )
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    remediationPathLoading.value = false
  }
}

async function loadTeacherProfiles() {
  if (!selectedCourse.value || !currentUser.value) return
  teacherProfileLoading.value = true
  try {
    teacherProfiles.value = await listTeacherProfiles(selectedCourse.value.id, currentUser.value.id)
    if (teacherProfiles.value.length > 0) {
      await selectTeacherProfile(teacherProfiles.value[0])
    } else {
      selectedTeacherProfile.value = null
      teacherProfileEvidence.value = []
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherProfileLoading.value = false
  }
}

async function runTeacherProfileAnalysis() {
  if (!selectedCourse.value || !currentUser.value) return
  if (!teacherProfileForm.teacherName.trim()) {
    ElMessage.warning('请输入教师名称')
    return
  }
  teacherProfileAnalyzing.value = true
  try {
    const profile = await analyzeTeacherProfile(selectedCourse.value.id, {
      userId: currentUser.value.id,
      teacherName: teacherProfileForm.teacherName.trim(),
      materialIds: teacherProfileForm.materialIds
    })
    teacherProfiles.value = [profile, ...teacherProfiles.value.filter(item => item.id !== profile.id)]
    await selectTeacherProfile(profile)
    resetTeacherProfileForm()
    ElMessage.success('教师画像分析完成')
  } catch (error) {
    await loadTeacherProfiles()
    ElMessage.error(error.message)
  } finally {
    await loadAiGenerationTasks()
    teacherProfileAnalyzing.value = false
  }
}

async function selectTeacherProfile(profile) {
  selectedTeacherProfile.value = profile
  teacherProfileEditing.value = false
  mockExamResult.value = null
  sprintOutlineResult.value = null
  teacherEvidenceLoading.value = true
  try {
    teacherProfileEvidence.value = await listTeacherProfileEvidence(profile.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherEvidenceLoading.value = false
  }
}

async function recalculateTeacherConfidence() {
  if (!selectedTeacherProfile.value || !currentUser.value) return
  teacherConfidenceScoring.value = true
  try {
    const profile = await recalculateTeacherProfileConfidence(
      selectedTeacherProfile.value.id,
      currentUser.value.id
    )
    replaceItem(teacherProfiles.value, profile)
    selectedTeacherProfile.value = profile
    ElMessage.success('教师画像置信度已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherConfidenceScoring.value = false
  }
}

async function runTeacherProfileReanalysis() {
  if (!selectedTeacherProfile.value || !currentUser.value) return
  teacherProfileReanalyzing.value = true
  try {
    const profile = await reanalyzeTeacherProfile(selectedTeacherProfile.value.id, {
      userId: currentUser.value.id,
      materialIds: [],
      model: 'deepseek-v4-flash'
    })
    replaceItem(teacherProfiles.value, profile)
    await selectTeacherProfile(profile)
    await loadAiGenerationTasks()
    ElMessage.success('教师画像已重新分析')
  } catch (error) {
    await loadTeacherProfiles()
    await loadAiGenerationTasks()
    ElMessage.error(error.message)
  } finally {
    teacherProfileReanalyzing.value = false
  }
}

async function generateMockExamFromProfile() {
  if (!selectedCourse.value || !currentUser.value) return
  const teacherProfileId = reviewProfileForm.teacherProfileId || selectedTeacherProfile.value?.id
  if (!teacherProfileId) {
    ElMessage.warning('请先在复习配置中选择教师画像')
    return
  }
  mockExamGenerating.value = true
  try {
    const result = await generateMockExam(selectedCourse.value.id, {
      userId: currentUser.value.id,
      teacherProfileId,
      questionCount: mockExamForm.questionCount,
      difficultyLevel: mockExamForm.difficultyLevel,
      includePrerequisites: reviewProfileForm.includePrerequisites,
      model: 'deepseek-v4-flash',
      customRequirement: mockExamForm.customRequirement.trim()
        || reviewProfileForm.customRequirement.trim()
        || null
    })
    mockExamResult.value = result
    activeReviewOutputType.value = 'MOCK_EXAM'
    addGeneratedMaterial(result.material)
    await loadAiGenerationTasks()
    await loadCourseStats()
    ElMessage.success(`已生成 ${result.questionCount} 道模拟题`)
  } catch (error) {
    await loadAiGenerationTasks()
    ElMessage.error(error.message)
  } finally {
    mockExamGenerating.value = false
  }
}

function downloadMockExam(task) {
  if (!task || !currentUser.value) return
  window.open(mockExamDownloadUrl(task.id, currentUser.value.id), '_blank')
}

async function generateSprintOutlineFromProfile() {
  if (!selectedCourse.value || !currentUser.value) return
  const teacherProfileId = reviewProfileForm.teacherProfileId || selectedTeacherProfile.value?.id
  if (!teacherProfileId) {
    ElMessage.warning('请先在复习配置中选择教师画像')
    return
  }
  sprintOutlineGenerating.value = true
  try {
    const result = await generateSprintOutline(selectedCourse.value.id, {
      userId: currentUser.value.id,
      teacherProfileId,
      days: sprintOutlineForm.days,
      model: 'deepseek-v4-flash',
      customRequirement: sprintOutlineForm.customRequirement.trim()
        || reviewProfileForm.customRequirement.trim()
        || null
    })
    sprintOutlineResult.value = result
    activeReviewOutputType.value = 'OUTLINE'
    await loadAiGenerationTasks()
    ElMessage.success(`已生成 ${result.dayCount} 天冲刺提纲`)
  } catch (error) {
    await loadAiGenerationTasks()
    ElMessage.error(error.message)
  } finally {
    sprintOutlineGenerating.value = false
  }
}

function downloadSprintOutline(task) {
  if (!task || !currentUser.value) return
  window.open(sprintOutlineDownloadUrl(task.id, currentUser.value.id), '_blank')
}

function selectReviewOutput(outputType) {
  activeReviewOutputType.value = outputType
  reviewProfileForm.outputType = outputType
}

async function generateSelectedReviewAsset() {
  if (activeReviewOutputType.value === 'MOCK_EXAM') {
    await generateMockExamFromProfile()
    return
  }
  await generateReviewAssetFromProfile()
}

async function generateReviewAssetFromProfile() {
  if (!selectedCourse.value || !currentUser.value) return
  reviewAssetGenerating.value = true
  try {
    const result = await generateReviewAsset(selectedCourse.value.id, {
      userId: currentUser.value.id,
      teacherProfileId: reviewProfileForm.teacherProfileId || selectedTeacherProfile.value?.id || null,
      outputType: activeReviewOutputType.value,
      difficultyLevel: reviewProfileForm.difficultyLevel,
      includePrerequisites: reviewProfileForm.includePrerequisites,
      model: 'deepseek-v4-flash',
      customRequirement: activeReviewCustomRequirement.value.trim()
        || reviewProfileForm.customRequirement.trim()
        || null
    })
    reviewAssetResult.value = result
    addGeneratedMaterial(result.material)
    await loadAiGenerationTasks()
    await loadCourseStats()
    ElMessage.success(`${reviewOutputLabel(result.outputType)}已生成`)
  } catch (error) {
    await loadAiGenerationTasks()
    ElMessage.error(error.message)
  } finally {
    reviewAssetGenerating.value = false
  }
}

function downloadReviewResult(result) {
  if (!result?.task || !currentUser.value) return
  if (result.outputType === 'MOCK_EXAM') {
    downloadMockExam(result.task)
    return
  }
  window.open(reviewAssetDownloadUrl(result.task.id, currentUser.value.id), '_blank')
}

async function loadReviewProfiles() {
  if (!selectedCourse.value || !currentUser.value) return
  reviewProfileLoading.value = true
  try {
    reviewProfiles.value = await listReviewProfiles(currentUser.value.id, selectedCourse.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reviewProfileLoading.value = false
  }
}

async function saveReviewProfile() {
  if (!selectedCourse.value || !currentUser.value) return
  if (!reviewProfileForm.profileName.trim()) {
    ElMessage.warning('请输入配置名称')
    return
  }
  reviewProfileSaving.value = true
  try {
    const payload = {
      userId: currentUser.value.id,
      courseId: selectedCourse.value.id,
      teacherProfileId: reviewProfileForm.teacherProfileId,
      profileName: reviewProfileForm.profileName.trim(),
      target: reviewProfileForm.target.trim() || null,
      difficultyLevel: reviewProfileForm.difficultyLevel,
      outputType: activeReviewOutputType.value,
      includePrerequisites: reviewProfileForm.includePrerequisites,
      customRequirement: reviewProfileForm.customRequirement.trim() || null
    }
    const saved = editingReviewProfileId.value
      ? await updateReviewProfile(editingReviewProfileId.value, payload)
      : await createReviewProfile(payload)
    if (editingReviewProfileId.value) {
      replaceItem(reviewProfiles.value, saved)
    } else {
      reviewProfiles.value.unshift(saved)
    }
    resetReviewProfileForm()
    ElMessage.success('复习配置已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reviewProfileSaving.value = false
  }
}

function editReviewProfile(profile) {
  editingReviewProfileId.value = profile.id
  reviewProfileForm.profileName = profile.profileName || ''
  reviewProfileForm.target = profile.target || ''
  reviewProfileForm.difficultyLevel = profile.difficultyLevel || 'MEDIUM'
  reviewProfileForm.outputType = profile.outputType || 'REVIEW_NOTE'
  activeReviewOutputType.value = reviewProfileForm.outputType
  reviewProfileForm.includePrerequisites = profile.includePrerequisites !== false
  reviewProfileForm.teacherProfileId = profile.teacherProfileId || null
  reviewProfileForm.customRequirement = profile.customRequirement || ''
}

async function removeReviewProfile(profile) {
  try {
    await deleteReviewProfile(profile.id, currentUser.value.id)
    reviewProfiles.value = reviewProfiles.value.filter(item => item.id !== profile.id)
    if (editingReviewProfileId.value === profile.id) {
      resetReviewProfileForm()
    }
    ElMessage.success('复习配置已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadAiProviders() {
  if (!currentUser.value) return
  aiProviderLoading.value = true
  try {
    const [status, providers] = await Promise.all([
      getAiStatus(),
      listAiProviders(currentUser.value.id)
    ])
    aiDefaultStatus.value = status
    aiProviders.value = providers
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    aiProviderLoading.value = false
  }
}

async function loadAiGenerationTasks() {
  if (!currentUser.value || !selectedCourse.value) return
  aiTaskLoading.value = true
  try {
    aiGenerationTasks.value = await listAiGenerationTasks(
      currentUser.value.id,
      selectedCourse.value.id
    )
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    aiTaskLoading.value = false
  }
}

async function saveAiProvider() {
  if (!currentUser.value) return
  if (!aiProviderForm.providerName.trim() || !aiProviderForm.baseUrl.trim() || !aiProviderForm.modelName.trim()) {
    ElMessage.warning('请填写供应商名称、Base URL 和模型名称')
    return
  }
  aiProviderSaving.value = true
  try {
    const payload = {
      userId: currentUser.value.id,
      providerName: aiProviderForm.providerName.trim(),
      baseUrl: aiProviderForm.baseUrl.trim(),
      modelName: aiProviderForm.modelName.trim(),
      apiKeyAlias: aiProviderForm.apiKeyAlias.trim() || null,
      enabled: aiProviderForm.enabled
    }
    const saved = editingAiProviderId.value
      ? await updateAiProvider(editingAiProviderId.value, payload)
      : await createAiProvider(payload)
    if (editingAiProviderId.value) {
      replaceItem(aiProviders.value, saved)
    } else {
      aiProviders.value.unshift(saved)
    }
    resetAiProviderForm()
    ElMessage.success('AI 配置已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    aiProviderSaving.value = false
  }
}

function editAiProvider(config) {
  editingAiProviderId.value = config.id
  aiProviderForm.providerName = config.providerName || ''
  aiProviderForm.baseUrl = config.baseUrl || ''
  aiProviderForm.modelName = config.modelName || ''
  aiProviderForm.apiKeyAlias = config.apiKeyAlias || ''
  aiProviderForm.enabled = config.enabled !== false
}

async function removeAiProvider(config) {
  try {
    await deleteAiProvider(config.id, currentUser.value.id)
    aiProviders.value = aiProviders.value.filter(item => item.id !== config.id)
    if (editingAiProviderId.value === config.id) {
      resetAiProviderForm()
    }
    ElMessage.success('AI 配置已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openTeacherProfileEditor() {
  if (!selectedTeacherProfile.value) return
  teacherEditForm.teacherName = selectedTeacherProfile.value.teacherName || ''
  teacherEditForm.confidenceScore = selectedTeacherProfile.value.confidenceScore ?? 50
  teacherEditForm.examStyle = selectedTeacherProfile.value.examStyle || ''
  teacherEditForm.questionPreference = selectedTeacherProfile.value.questionPreference || ''
  teacherEditForm.gradingPreference = selectedTeacherProfile.value.gradingPreference || ''
  teacherEditForm.focusTopics = selectedTeacherProfile.value.focusTopics || ''
  teacherEditForm.avoidTopics = selectedTeacherProfile.value.avoidTopics || ''
  teacherEditForm.sourceSummary = selectedTeacherProfile.value.sourceSummary || ''
  teacherProfileEditing.value = true
}

function cancelTeacherProfileEdit() {
  teacherProfileEditing.value = false
}

async function saveTeacherProfileEdit() {
  if (!selectedTeacherProfile.value || !currentUser.value) return
  if (!teacherEditForm.teacherName.trim()) {
    ElMessage.warning('请输入教师名称')
    return
  }
  teacherProfileSaving.value = true
  try {
    const profile = await updateTeacherProfile(selectedTeacherProfile.value.id, {
      userId: currentUser.value.id,
      teacherName: teacherEditForm.teacherName.trim(),
      confidenceScore: teacherEditForm.confidenceScore,
      examStyle: teacherEditForm.examStyle,
      questionPreference: teacherEditForm.questionPreference,
      gradingPreference: teacherEditForm.gradingPreference,
      focusTopics: teacherEditForm.focusTopics,
      avoidTopics: teacherEditForm.avoidTopics,
      sourceSummary: teacherEditForm.sourceSummary,
      analysisStatus: 'MANUAL_REVIEWED'
    })
    replaceItem(teacherProfiles.value, profile)
    selectedTeacherProfile.value = profile
    teacherProfileEditing.value = false
    ElMessage.success('教师画像已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherProfileSaving.value = false
  }
}

async function runExport() {
  const payload = buildExportPayload()
  if (!payload) return
  exportCreating.value = true
  try {
    const record = await createExport(payload)
    exportRecords.value.unshift(record)
    exportPreview.value = null
    await loadCourseStats()
    ElMessage.success('知识包已生成')
    downloadExport(record)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exportCreating.value = false
  }
}

async function runExportPreview() {
  const payload = buildExportPayload()
  if (!payload) return
  exportPreviewLoading.value = true
  try {
    exportPreview.value = await previewExport(payload)
    ElMessage.success('导出预览已生成')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exportPreviewLoading.value = false
  }
}

function buildExportPayload() {
  if (!selectedCourse.value || !currentUser.value) return null
  if (!exportForm.exportName.trim()) {
    ElMessage.warning('请输入导出名称')
    return null
  }
  return {
    userId: currentUser.value.id,
    courseId: selectedCourse.value.id,
    templateId: exportForm.templateId,
    exportName: exportForm.exportName.trim(),
    exportFormat: 'ZIP',
    chapterIds: exportForm.chapterIds,
    materialTypes: exportForm.materialTypes,
    onlyKeyMaterials: exportForm.onlyKeyMaterials,
    includeExamStats: exportForm.includeExamStats,
    includePrerequisiteCourses: exportForm.includePrerequisiteCourses
      && hasCourseRelationsOfType('PREREQUISITE'),
    includeRelatedCourses: exportForm.includeRelatedCourses
      && hasCourseRelationsOfType('RELATED'),
    includeFollowUpCourses: exportForm.includeFollowUpCourses
      && hasCourseRelationsOfType('FOLLOW_UP')
  }
}

function downloadExport(record) {
  window.open(exportDownloadUrl(record.id, currentUser.value.id), '_blank')
}

async function setRecommendedExport(record) {
  try {
    const updated = await markExportRecommended(record.id, currentUser.value.id)
    exportRecords.value = exportRecords.value.map(item => ({
      ...item,
      recommended: item.id === updated.id
    }))
    ElMessage.success(`v${updated.versionNo || 1} 已设为推荐版本`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function formatExportTime(value) {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString()
}

async function loadKnowledgeItems() {
  if (!selectedCourse.value) return
  knowledgeLoading.value = true
  try {
    knowledgeItems.value = await listKnowledgeItems(
      selectedCourse.value.id,
      currentUser.value.id,
      {
        itemType: knowledgeFilterType.value || undefined
      }
    )
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    knowledgeLoading.value = false
  }
}

async function loadCourseTags() {
  if (!selectedCourse.value) return
  try {
    courseTags.value = await listCourseTags(selectedCourse.value.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadSelectedMaterialTags() {
  previewedMaterialTags.value = []
  if (!knowledgeForm.materialId) {
    selectedMaterialTags.value = []
    return
  }
  try {
    selectedMaterialTags.value = await listMaterialTags(
      knowledgeForm.materialId,
      currentUser.value.id
    )
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function saveSelectedMaterialTags() {
  if (!knowledgeForm.materialId) return
  tagSaving.value = true
  try {
    selectedMaterialTags.value = await replaceMaterialTags(
      knowledgeForm.materialId,
      currentUser.value.id,
      selectedMaterialTags.value
    )
    await loadCourseTags()
    ElMessage.success('标签已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    tagSaving.value = false
  }
}

async function previewTags() {
  if (!knowledgeForm.materialId) return
  tagPreviewLoading.value = true
  try {
    const result = await previewMaterialTags(
      knowledgeForm.materialId,
      currentUser.value.id,
      { model: 'deepseek-v4-flash' }
    )
    previewedMaterialTags.value = result.tags || []
    selectedMaterialTags.value = Array.from(new Set([
      ...selectedMaterialTags.value,
      ...previewedMaterialTags.value
    ]))
    await loadAiGenerationTasks()
    if (previewedMaterialTags.value.length === 0) {
      ElMessage.warning('AI 未提取到有效关键词')
    } else {
      ElMessage.success(`已提取 ${previewedMaterialTags.value.length} 个候选关键词`)
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    tagPreviewLoading.value = false
  }
}

function togglePreviewTag(tag) {
  if (selectedMaterialTags.value.includes(tag)) {
    selectedMaterialTags.value = selectedMaterialTags.value.filter(item => item !== tag)
  } else {
    selectedMaterialTags.value = [...selectedMaterialTags.value, tag]
  }
}

async function applyPreviewTags() {
  if (!knowledgeForm.materialId || previewedMaterialTags.value.length === 0) return
  await saveSelectedMaterialTags()
}

async function generateKnowledge() {
  if (!knowledgeForm.materialId) {
    ElMessage.warning('请选择已解析资料')
    return
  }
  knowledgeGenerating.value = true
  try {
    const result = await generateKnowledgeItems(
      knowledgeForm.materialId,
      currentUser.value.id,
      {
        maxItems: knowledgeForm.maxItems,
        replaceExisting: knowledgeForm.replaceExisting,
        model: 'deepseek-v4-flash'
      }
    )
    selectedMaterialTags.value = result.tags
    await Promise.all([loadKnowledgeItems(), loadCourseTags()])
    await loadCourseStats()
    ElMessage.success(`AI 已整理 ${result.items.length} 条知识`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    await loadAiGenerationTasks()
    knowledgeGenerating.value = false
  }
}

async function saveKnowledgeMastery(item, masteryStatus) {
  if (!currentUser.value || !item?.id) return
  if ((item.masteryStatus || 'UNKNOWN') === masteryStatus) return
  masterySavingIds.value = [...new Set([...masterySavingIds.value, item.id])]
  try {
    const status = await updateKnowledgeMastery(item.id, {
      userId: currentUser.value.id,
      masteryStatus: masteryStatus || 'UNKNOWN',
      masteryScore: null,
      note: null
    })
    item.masteryStatus = status.masteryStatus
    item.masteryScore = status.masteryScore
    item.masteryNote = status.note
    item.lastReviewTime = status.lastReviewTime
    item.masteryUpdateTime = status.updateTime
    ElMessage.success('掌握状态已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    masterySavingIds.value = masterySavingIds.value.filter(id => id !== item.id)
  }
}

function isMasterySaving(itemId) {
  return masterySavingIds.value.includes(itemId)
}

function formatReviewTime(value) {
  return value ? new Date(value).toLocaleString() : '尚未复习'
}

function formatSearchTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function searchTypeLabel(type) {
  if (type === 'UNIFIED_KNOWLEDGE') return '统一检索'
  return type || '检索'
}

function knowledgeTypeLabel(type) {
  return knowledgeTypeOptions.find(option => option.type === type)?.label || type
}

async function runSearch(options = {}) {
  const { fromQuick = false, showDetail = false } = options
  if (!selectedCourse.value) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (!searchForm.keyword.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  searchLoading.value = true
  try {
    const keyword = searchForm.keyword.trim()
    searchForm.keyword = keyword
    quickSearchKeyword.value = keyword
    if (fromQuick) {
      searchForm.chapterId = null
      searchForm.materialType = null
      searchForm.isKey = false
    }
    searchResults.value = await searchMaterials({
      userId: currentUser.value.id,
      courseId: selectedCourse.value.id,
      keyword,
      chapterId: searchForm.chapterId || undefined,
      materialType: searchForm.materialType || undefined,
      isKey: searchForm.isKey
    })
    searchHasRun.value = true
    if (showDetail) {
      activateSection('search')
    }
    await loadSearchRecords()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    searchLoading.value = false
  }
}

async function runQuickSearch() {
  if (!quickSearchKeyword.value.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  searchForm.keyword = quickSearchKeyword.value.trim()
  await runSearch({ fromQuick: true, showDetail: true })
}

function resetSearch() {
  searchForm.keyword = ''
  quickSearchKeyword.value = ''
  searchForm.chapterId = null
  searchForm.materialType = null
  searchForm.isKey = false
  searchResults.value = []
  searchHasRun.value = false
}

async function rerunSearchRecord(record) {
  if (!record?.keyword) return
  searchForm.keyword = record.keyword
  await runSearch()
}

function resetRelationForm() {
  relationForm.relatedCourseId = null
  relationForm.relationType = 'PREREQUISITE'
  relationForm.reason = ''
}

function resetGapForm() {
  gapForm.reportName = selectedCourse.value
    ? `${selectedCourse.value.courseName} 知识缺口报告`
    : ''
  gapForm.includePrerequisites = true
}

function clearGapState() {
  gapReports.value = []
  gapItems.value = []
  prerequisiteGapHints.value = []
  remediationPath.value = null
  selectedGapReport.value = null
  resetGapForm()
}

function resetTeacherProfileForm() {
  teacherProfileForm.teacherName = ''
  teacherProfileForm.materialIds = []
}

function resetMockExamForm() {
  mockExamForm.questionCount = 8
  mockExamForm.difficultyLevel = 'MEDIUM'
  mockExamForm.customRequirement = ''
  mockExamResult.value = null
}

function resetSprintOutlineForm() {
  sprintOutlineForm.days = 7
  sprintOutlineForm.customRequirement = ''
  sprintOutlineResult.value = null
}

function clearTeacherProfileState() {
  teacherProfiles.value = []
  teacherProfileEvidence.value = []
  selectedTeacherProfile.value = null
  reviewAssetResult.value = null
  resetTeacherProfileForm()
  resetMockExamForm()
  resetSprintOutlineForm()
}

function resetReviewProfileForm() {
  editingReviewProfileId.value = null
  reviewProfileForm.profileName = selectedCourse.value
    ? `${selectedCourse.value.courseName} 复习配置`
    : ''
  reviewProfileForm.target = ''
  reviewProfileForm.difficultyLevel = 'MEDIUM'
  reviewProfileForm.outputType = 'REVIEW_NOTE'
  activeReviewOutputType.value = 'REVIEW_NOTE'
  reviewProfileForm.includePrerequisites = true
  reviewProfileForm.teacherProfileId = null
  reviewProfileForm.customRequirement = ''
}

function clearReviewProfileState() {
  reviewProfiles.value = []
  resetReviewProfileForm()
}

function resetAiProviderForm() {
  editingAiProviderId.value = null
  aiProviderForm.providerName = 'DeepSeek'
  aiProviderForm.baseUrl = 'https://api.deepseek.com'
  aiProviderForm.modelName = 'deepseek-v4-flash'
  aiProviderForm.apiKeyAlias = 'DEEPSEEK_API_KEY'
  aiProviderForm.enabled = true
}

function clearAiProviderState() {
  aiProviders.value = []
  aiDefaultStatus.value = null
  aiGenerationTasks.value = []
  resetAiProviderForm()
}

function resetExportForm() {
  exportForm.exportName = selectedCourse.value
    ? `${selectedCourse.value.courseName} 知识包`
    : ''
  exportForm.templateId = exportTemplates.value[0]?.id || null
  exportForm.chapterIds = []
  exportForm.materialTypes = []
  exportForm.onlyKeyMaterials = false
  exportForm.includeExamStats = true
  exportForm.includePrerequisiteCourses = false
  exportForm.includeRelatedCourses = false
  exportForm.includeFollowUpCourses = false
  exportPreview.value = null
}

function hasCourseRelationsOfType(type) {
  return courseRelations.value.some(relation => relation.relationType === type)
}

function materialTypeLabel(type) {
  return materialTypeOptions.find(option => option.type === type)?.label || type
}

function masteryStatusLabel(status) {
  return masteryStatusOptions.find(option => option.status === status)?.label || '未评估'
}

function gapTypeLabel(type) {
  if (type === 'WEAK_MASTERY') return '学习中'
  if (type === 'HIGH_FREQUENCY') return '高频考点'
  if (type === 'PREREQUISITE_GAP') return '前置缺口'
  return '未评估'
}

function relationTypeLabel(type) {
  return relationTypeOptions.find(option => option.type === type)?.label || '当前课程'
}

function formatGapTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function teacherStatusLabel(status) {
  if (status === 'RUNNING') return '分析中'
  if (status === 'SUCCESS') return '已完成'
  if (status === 'FAILED') return '失败'
  if (status === 'MANUAL_REVIEWED') return '已人工确认'
  return '等待分析'
}

function evidenceTypeLabel(type) {
  if (type === 'QUESTION_TYPE') return '题型证据'
  if (type === 'GRADING') return '评分证据'
  if (type === 'FOCUS_TOPIC') return '重点证据'
  if (type === 'AVOID_TOPIC') return '规避证据'
  return '风格证据'
}

function reviewDifficultyLabel(value) {
  return reviewDifficultyOptions.find(option => option.value === value)?.label || value
}

function reviewOutputLabel(value) {
  return reviewOutputOptions.find(option => option.value === value)?.label || value
}

function normalizeReviewResult(result, outputType) {
  if (!result) return null
  return {
    ...result,
    outputType,
    content: result.content || ''
  }
}

function renderMarkdown(markdown) {
  if (!markdown) return ''
  const lines = markdown.replace(/\r\n/g, '\n').split('\n')
  const html = []
  let listOpen = false
  let tableBuffer = []

  const closeList = () => {
    if (listOpen) {
      html.push('</ul>')
      listOpen = false
    }
  }
  const flushTable = () => {
    if (tableBuffer.length === 0) return
    html.push(renderMarkdownTable(tableBuffer))
    tableBuffer = []
  }

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) {
      closeList()
      flushTable()
      continue
    }
    if (trimmed.includes('|') && trimmed.startsWith('|')) {
      closeList()
      tableBuffer.push(trimmed)
      continue
    }
    flushTable()
    const heading = trimmed.match(/^(#{1,4})\s+(.+)$/)
    if (heading) {
      closeList()
      const level = heading[1].length
      html.push(`<h${level}>${inlineMarkdown(heading[2])}</h${level}>`)
      continue
    }
    const checkbox = trimmed.match(/^[-*]\s+\[( |x|X)]\s+(.+)$/)
    if (checkbox) {
      if (!listOpen) {
        html.push('<ul>')
        listOpen = true
      }
      const checked = checkbox[1].toLowerCase() === 'x'
      html.push(`<li class="markdown-check ${checked ? 'checked' : ''}">${inlineMarkdown(checkbox[2])}</li>`)
      continue
    }
    const listItem = trimmed.match(/^[-*]\s+(.+)$/)
    if (listItem) {
      if (!listOpen) {
        html.push('<ul>')
        listOpen = true
      }
      html.push(`<li>${inlineMarkdown(listItem[1])}</li>`)
      continue
    }
    closeList()
    html.push(`<p>${inlineMarkdown(trimmed)}</p>`)
  }
  closeList()
  flushTable()
  return html.join('')
}

function renderMarkdownTable(lines) {
  const rows = lines
    .filter(line => !/^\|\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?$/.test(line))
    .map(line => line.replace(/^\||\|$/g, '').split('|').map(cell => inlineMarkdown(cell.trim())))
  if (rows.length === 0) return ''
  const [head, ...body] = rows
  return `<table><thead><tr>${head.map(cell => `<th>${cell}</th>`).join('')}</tr></thead><tbody>${body.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table>`
}

function inlineMarkdown(value) {
  return escapeHtml(value)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function aiTaskTypeLabel(type) {
  if (type === 'TEACHER_PROFILE') return '教师画像分析'
  if (type === 'EXAM_MAPPING') return '真题知识映射'
  if (type === 'KNOWLEDGE_GAP') return '知识缺口检测'
  if (type === 'REVIEW_GENERATION') return '复习资料生成'
  if (type === 'MOCK_EXAM') return '模拟题生成'
  if (type === 'PACKAGE_SUMMARY') return '知识包摘要'
  if (type === 'KNOWLEDGE_EXTRACTION') return '资料知识整理'
  if (type === 'MATERIAL_SUMMARY') return '资料摘要生成'
  if (type === 'MATERIAL_TAG_EXTRACTION') return '资料关键词提取'
  return type || '未知任务'
}

function aiTaskStatusLabel(status) {
  if (status === 'RUNNING') return '运行中'
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'CANCELED') return '已取消'
  return '等待中'
}

function formatAiTaskTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function matchSourceLabel(source) {
  if (source === 'TITLE') return '标题命中'
  if (source === 'SUMMARY') return '摘要命中'
  if (source === 'TAG') return '标签命中'
  if (source === 'KNOWLEDGE_TITLE') return '知识标题命中'
  if (source === 'KNOWLEDGE_CONTENT') return '知识内容命中'
  return '正文命中'
}

function searchResultMaterial(result) {
  return materials.value.find(material => material.id === result.materialId) || {
    id: result.materialId,
    courseId: result.courseId,
    chapterId: result.chapterId,
    title: result.title,
    materialType: result.materialType,
    summary: result.summary,
    year: result.year,
    key: result.key,
    originalName: result.originalName,
    fileType: result.fileType,
    fileSize: result.fileSize,
    parsedChunkCount: result.parsedChunkCount
  }
}

function editSearchResult(result) {
  openMaterialEditor(searchResultMaterial(result))
}

function previewSearchResult(result) {
  showParsedText(searchResultMaterial(result))
}

function openKnowledgeResult() {
  activateSection('knowledge')
}

async function saveMaterial() {
  if (!editingMaterialId.value && uploadDrafts.value.length === 0) {
    ElMessage.warning('请选择资料文件')
    return
  }
  if (editingMaterialId.value && !materialForm.title.trim()) {
    ElMessage.warning('请输入资料标题')
    return
  }
  if (!editingMaterialId.value) {
    const missingTitle = uploadDrafts.value.find(draft => !draft.title.trim())
    if (missingTitle) {
      ElMessage.warning(`请填写「${missingTitle.name}」的资料标题`)
      return
    }
    const missingFile = uploadDrafts.value.find(draft => !draft.file)
    if (missingFile) {
      ElMessage.warning(`请重新选择「${missingFile.name}」`)
      return
    }
  }

  materialSaving.value = true
  try {
    if (editingMaterialId.value) {
      const updated = await updateMaterial(
        editingMaterialId.value,
        currentUser.value.id,
        {
          title: materialForm.title.trim(),
          materialType: materialForm.materialType,
          chapterId: materialForm.chapterId,
          year: materialForm.year,
          key: materialForm.isKey,
          summary: materialForm.summary.trim() || null
        }
      )
      replaceItem(materials.value, updated)
      await loadCourseStats()
      materialDialogVisible.value = false
      ElMessage.success('资料信息已更新')
      return
    }

    const data = new FormData()
    data.append('userId', currentUser.value.id)
    data.append('courseId', selectedCourse.value.id)
    uploadDrafts.value.forEach(draft => {
      data.append('files', draft.file)
      data.append('titles', draft.title.trim())
      data.append('materialTypes', draft.materialType)
      data.append('chapterIds', draft.chapterId || '')
      data.append('years', draft.year || '')
      data.append('isKeys', draft.isKey)
      data.append('summaries', draft.summary.trim())
    })

    const uploadedMaterials = await uploadMaterialsBatch(data)
    materials.value.unshift(...uploadedMaterials)
    await loadCourseStats()
    await checkSimilarMaterials(uploadedMaterials)
    resetMaterialForm()
    materialDialogVisible.value = false
    ElMessage.success(`已上传 ${uploadedMaterials.length} 份资料`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    materialSaving.value = false
  }
}

function handleFileChange(uploadFile, uploadFiles) {
  syncUploadDrafts(uploadFiles)
}

function handleFileRemove(uploadFile, uploadFiles) {
  syncUploadDrafts(uploadFiles)
}

function syncUploadDrafts(uploadFiles = []) {
  uploadFileList.value = uploadFiles
  const existingDrafts = new Map(uploadDrafts.value.map(draft => [draft.uid, draft]))
  uploadDrafts.value = uploadFiles.map(uploadFile => {
    const existing = existingDrafts.get(uploadFile.uid)
    if (existing) {
      existing.file = uploadFile.raw
      existing.name = uploadFile.name
      existing.size = uploadFile.size || uploadFile.raw?.size || 0
      return existing
    }
    return createUploadDraft(uploadFile)
  })
}

function createUploadDraft(uploadFile) {
  const name = uploadFile.name || uploadFile.raw?.name || ''
  return {
    uid: uploadFile.uid,
    file: uploadFile.raw,
    name,
    size: uploadFile.size || uploadFile.raw?.size || 0,
    title: name.replace(/\.[^.]+$/, ''),
    materialType: materialForm.materialType,
    chapterId: materialForm.chapterId,
    year: materialForm.year,
    isKey: materialForm.isKey,
    summary: materialForm.summary
  }
}

async function runMaterialParse(material) {
  parsingMaterialId.value = material.id
  try {
    const result = await parseMaterial(material.id, currentUser.value.id)
    material.parsedChunkCount = result.chunkCount
    await loadCourseStats()
    await checkSimilarMaterials([material])
    ElMessage.success(`解析完成，共提取 ${result.chunkCount} 个文本块`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    parsingMaterialId.value = null
  }
}

function isSummaryGenerating(materialId) {
  return summaryGeneratingIds.value.includes(materialId)
}

async function runMaterialSummary(material) {
  if (!material.parsedChunkCount) {
    ElMessage.warning('请先解析资料文本')
    return
  }
  summaryGeneratingIds.value.push(material.id)
  try {
    const updated = await generateMaterialSummary(
      material.id,
      currentUser.value.id,
      { model: 'deepseek-v4-flash' }
    )
    replaceItem(materials.value, updated)
    await loadAiGenerationTasks()
    ElMessage.success('资料摘要已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    summaryGeneratingIds.value = summaryGeneratingIds.value.filter(id => id !== material.id)
  }
}

async function checkSimilarMaterials(sourceMaterials) {
  for (const material of sourceMaterials) {
    try {
      const results = await listSimilarMaterials(material.id, currentUser.value.id)
      if (results.length > 0) {
        similarSourceMaterial.value = material
        similarMaterialResults.value = results
        similarDialogVisible.value = true
        return
      }
    } catch (error) {
      ElMessage.warning(`相似资料检测失败：${error.message}`)
    }
  }
}

function openSimilarMaterial(material) {
  similarDialogVisible.value = false
  openMaterialReader(material)
}

function addGeneratedMaterial(material) {
  if (!material?.id) return
  const index = materials.value.findIndex(item => item.id === material.id)
  if (index >= 0) {
    materials.value.splice(index, 1, material)
    return
  }
  materials.value.unshift(material)
}

function materialFileType(material) {
  return (material?.fileType || '').toLowerCase()
}

function canParseMaterial(material) {
  return ['pdf', 'doc', 'docx', 'md', 'txt'].includes(materialFileType(material))
}

async function openMaterialReader(material) {
  if (!material || !currentUser.value) return
  previewMaterial.value = material
  textPreviewVisible.value = true
  textChunkLoading.value = true
  textChunks.value = []
  materialPreviewContent.value = ''
  const fileType = materialFileType(material)
  try {
    if (fileType === 'pdf') {
      materialPreviewMode.value = 'pdf'
      return
    }
    if (['md', 'txt', 'doc', 'docx'].includes(fileType)) {
      materialPreviewMode.value = fileType === 'md' ? 'markdown' : 'text'
      materialPreviewContent.value = await previewMaterialContent(material.id, currentUser.value.id)
      return
    }
    if (material.parsedChunkCount > 0) {
      materialPreviewMode.value = 'chunks'
      textChunks.value = await listTextChunks(material.id, currentUser.value.id)
      return
    }
    materialPreviewMode.value = 'download'
  } catch (error) {
    materialPreviewMode.value = 'download'
    ElMessage.error(error.message)
  } finally {
    textChunkLoading.value = false
  }
}

function downloadPreviewMaterial() {
  if (!previewMaterial.value || !currentUser.value) return
  window.open(materialFileUrl(previewMaterial.value.id, currentUser.value.id, 'attachment'), '_blank')
}

async function showParsedText(material) {
  previewMaterial.value = material
  textPreviewVisible.value = true
  materialPreviewMode.value = 'chunks'
  materialPreviewContent.value = ''
  textChunkLoading.value = true
  try {
    textChunks.value = await listTextChunks(material.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    textChunkLoading.value = false
  }
}

function openExamWorkbench(material) {
  examPreferredMaterialId.value = material.id
  scrollToSection('exam')
}

function handleExamMaterialParsed({ materialId, chunkCount }) {
  const target = materials.value.find(item => item.id === materialId)
  if (target) {
    target.parsedChunkCount = chunkCount
  }
  loadCourseStats()
}

async function saveCourse() {
  if (!courseForm.courseName.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }

  courseSaving.value = true
  try {
    const payload = {
      userId: currentUser.value.id,
      courseName: courseForm.courseName.trim(),
      courseCode: courseForm.courseCode.trim() || null,
      semester: courseForm.semester.trim() || null,
      description: courseForm.description.trim() || null
    }
    const course = editingCourseId.value
      ? await updateCourse(editingCourseId.value, payload)
      : await createCourse(payload)

    if (editingCourseId.value) {
      replaceItem(courses.value, course)
      selectedCourse.value = course
      courseDialogVisible.value = false
      ElMessage.success('课程信息已更新')
      return
    }

    courses.value.unshift(course)
    resetCourseForm()
    courseDialogVisible.value = false
    await selectCourse(course)
    ElMessage.success('课程创建成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseSaving.value = false
  }
}

async function saveChapter() {
  if (!chapterForm.chapterNo.trim() || !chapterForm.chapterTitle.trim()) {
    ElMessage.warning('请填写章节编号和名称')
    return
  }

  chapterSaving.value = true
  try {
    const payload = {
      chapterNo: chapterForm.chapterNo.trim(),
      chapterTitle: chapterForm.chapterTitle.trim(),
      sortOrder: chapterForm.sortOrder
    }
    const chapter = editingChapterId.value
      ? await updateChapter(
          selectedCourse.value.id,
          editingChapterId.value,
          currentUser.value.id,
          payload
        )
      : await createChapter(selectedCourse.value.id, currentUser.value.id, payload)

    if (editingChapterId.value) {
      replaceItem(chapters.value, chapter)
      chapterDialogVisible.value = false
      ElMessage.success('章节信息已更新')
    } else {
      chapters.value.push(chapter)
      chapterDialogVisible.value = false
      ElMessage.success('章节添加成功')
    }
    chapters.value.sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    chapterSaving.value = false
  }
}

function confirmCourseDeletion() {
  deleteTarget.value = {
    type: 'course',
    id: selectedCourse.value.id,
    name: selectedCourse.value.courseName
  }
  deleteDialogVisible.value = true
}

function confirmChapterDeletion(chapter = null) {
  const target = chapter || chapters.value.find(item => item.id === editingChapterId.value)
  if (!target) return
  deleteTarget.value = {
    type: 'chapter',
    id: target.id,
    name: `${target.chapterNo} ${target.chapterTitle}`
  }
  deleteDialogVisible.value = true
}

function confirmMaterialDeletion(material = null) {
  const target = material || materials.value.find(item => item.id === editingMaterialId.value)
  if (!target) return
  deleteTarget.value = {
    type: 'material',
    id: target.id,
    name: target.title
  }
  deleteDialogVisible.value = true
}

function confirmKnowledgeDeletion(item) {
  deleteTarget.value = {
    type: 'knowledge',
    id: item.id,
    name: item.title
  }
  deleteDialogVisible.value = true
}

async function executeDeletion() {
  if (!deleteTarget.value) return
  deleteLoading.value = true
  try {
    if (deleteTarget.value.type === 'course') {
      await removeCourse(deleteTarget.value.id)
    } else if (deleteTarget.value.type === 'chapter') {
      await removeChapter(deleteTarget.value.id)
    } else if (deleteTarget.value.type === 'knowledge') {
      await removeKnowledgeItem(deleteTarget.value.id)
    } else {
      await removeMaterial(deleteTarget.value.id)
    }
  } finally {
    deleteLoading.value = false
  }
}

async function removeKnowledgeItem(itemId) {
  try {
    await deleteKnowledgeItem(
      selectedCourse.value.id,
      itemId,
      currentUser.value.id
    )
    knowledgeItems.value = knowledgeItems.value.filter(item => item.id !== itemId)
    await loadCourseStats()
    deleteDialogVisible.value = false
    ElMessage.success('知识条目已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeCourse(courseId) {
  try {
    await deleteCourse(courseId, currentUser.value.id)
    courses.value = courses.value.filter(course => course.id !== courseId)
    courseDialogVisible.value = false
    deleteDialogVisible.value = false
    if (courses.value.length > 0) {
      await selectCourse(courses.value[0])
    } else {
      selectedCourse.value = null
      chapters.value = []
      materials.value = []
      courseRelations.value = []
      clearGapState()
      clearTeacherProfileState()
      clearReviewProfileState()
      clearAiProviderState()
    }
    ElMessage.success('课程已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeChapter(chapterId) {
  try {
    await deleteChapter(selectedCourse.value.id, chapterId, currentUser.value.id)
    chapters.value = chapters.value.filter(item => item.id !== chapterId)
    materials.value.forEach(material => {
      if (material.chapterId === chapterId) {
        material.chapterId = null
      }
    })
    chapterDialogVisible.value = false
    deleteDialogVisible.value = false
    ElMessage.success('章节已删除，相关资料已保留')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeMaterial(materialId) {
  try {
    await deleteMaterial(materialId, currentUser.value.id)
    materials.value = materials.value.filter(item => item.id !== materialId)
    await loadCourseStats()
    materialDialogVisible.value = false
    deleteDialogVisible.value = false
    ElMessage.success('资料已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function resetDeleteTarget() {
  deleteTarget.value = null
}

function logout() {
  localStorage.removeItem('ai4note-user')
  currentUser.value = null
  courses.value = []
  chapters.value = []
  materials.value = []
  courseRelations.value = []
  courseStats.value = null
  searchRecords.value = []
  selectedCourse.value = null
  clearGapState()
  clearTeacherProfileState()
  clearReviewProfileState()
  clearAiProviderState()
}

function openCourseCreator() {
  resetCourseForm()
  courseDialogVisible.value = true
}

function openCourseEditor() {
  editingCourseId.value = selectedCourse.value.id
  courseForm.courseName = selectedCourse.value.courseName || ''
  courseForm.courseCode = selectedCourse.value.courseCode || ''
  courseForm.semester = selectedCourse.value.semester || ''
  courseForm.description = selectedCourse.value.description || ''
  courseDialogVisible.value = true
}

function openChapterCreator() {
  resetChapterForm()
  chapterDialogVisible.value = true
}

function openChapterEditor(chapter) {
  editingChapterId.value = chapter.id
  chapterForm.chapterNo = chapter.chapterNo
  chapterForm.chapterTitle = chapter.chapterTitle
  chapterForm.sortOrder = chapter.sortOrder
  chapterDialogVisible.value = true
}

function openMaterialUploader(materialType = 'SLIDE') {
  resetMaterialForm()
  materialForm.materialType = typeof materialType === 'string' ? materialType : 'SLIDE'
  materialDialogVisible.value = true
}

function openMaterialEditor(material) {
  editingMaterialId.value = material.id
  materialForm.title = material.title
  materialForm.materialType = material.materialType
  materialForm.chapterId = material.chapterId
  materialForm.year = material.year
  materialForm.isKey = material.key
  materialForm.summary = material.summary || ''
  materialDialogVisible.value = true
}

function replaceItem(items, updated) {
  const index = items.findIndex(item => item.id === updated.id)
  if (index >= 0) {
    items.splice(index, 1, updated)
  }
}

function chapterName(chapterId) {
  if (!chapterId) return '未关联章节'
  const chapter = chapters.value.find(item => item.id === chapterId)
  return chapter ? `${chapter.chapterNo} ${chapter.chapterTitle}` : '未知章节'
}

function resetCourseForm() {
  editingCourseId.value = null
  courseForm.courseName = ''
  courseForm.courseCode = ''
  courseForm.semester = ''
  courseForm.description = ''
}

function resetChapterForm() {
  editingChapterId.value = null
  chapterForm.chapterNo = ''
  chapterForm.chapterTitle = ''
  chapterForm.sortOrder = 0
}

function resetMaterialForm() {
  materialUploadRef.value?.clearFiles()
  editingMaterialId.value = null
  materialForm.title = ''
  materialForm.materialType = 'SLIDE'
  materialForm.chapterId = null
  materialForm.year = new Date().getFullYear()
  materialForm.isKey = false
  materialForm.summary = ''
  uploadFileList.value = []
  uploadDrafts.value = []
}

function formatFileSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style src="./dashboard.css"></style>
