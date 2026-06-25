<template>
  <div class="exam-panel">
    <div class="exam-heading">
      <div>
        <p class="eyebrow">exam mapping</p>
        <h2>真题拆解、映射与统计<span>.</span></h2>
      </div>
      <p>先从往年真题资料里抽题，再自动映射到课程知识条目，最后按需要人工修正。</p>
    </div>

    <div class="exam-overview">
      <article class="exam-overview-card">
        <strong>{{ examMaterials.length }}</strong>
        <span>exam materials</span>
      </article>
      <article class="exam-overview-card">
        <strong>{{ questionTotal }}</strong>
        <span>extracted questions</span>
      </article>
      <article class="exam-overview-card">
        <strong>{{ stats.length }}</strong>
        <span>mapped hotspots</span>
      </article>
    </div>

    <section class="exam-control-panel">
      <div class="exam-material-strip">
        <div class="exam-strip-heading">
          <div>
            <p class="eyebrow">exam materials</p>
            <h3>选择真题资料并触发抽题</h3>
          </div>
          <span>{{ examMaterials.length }} 份</span>
        </div>
        <div class="exam-materials">
          <article
            v-for="material in examMaterials"
            :key="material.id"
            class="exam-material-card"
            :class="{ active: selectedMaterialIds.includes(material.id) }"
            @click="toggleMaterial(material.id)"
          >
            <div class="exam-material-copy">
              <strong>{{ material.title }}</strong>
              <small>{{ material.year || '未标年份' }} / {{ material.originalName }}</small>
            </div>
            <div class="exam-material-meta">
              <span>{{ (material.fileType || 'file').toUpperCase() }}</span>
              <span>{{ material.parsedChunkCount > 0 ? '已解析' : '未解析' }}</span>
              <span>{{ material.questionCount > 0 ? '已抽 ' + material.questionCount + ' 题' : '未抽题' }}</span>
            </div>
            <div class="exam-material-actions">
              <button
                type="button"
                :disabled="extractingMaterialIds.has(material.id)"
                @click.stop="handleExtract(material)"
              >
                {{ extractingMaterialIds.has(material.id) ? '抽取中…' : '抽取题目' }}
              </button>
            </div>
          </article>
          <div v-if="examMaterials.length === 0" class="exam-empty-card">
            <strong>还没有 EXAM 类型资料</strong>
            <p>请先上传往年真题资料，再进行题目抽取。</p>
          </div>
        </div>
      </div>

      <div class="exam-toolbar">
        <div class="exam-filter-field">
          <span>章节</span>
          <el-select
            v-model="filters.chapterIds"
            multiple
            clearable
            placeholder="全部章节"
            @change="reloadData"
          >
            <el-option
              v-for="chapter in chapters"
              :key="chapter.id"
              :label="`${chapter.chapterNo} ${chapter.chapterTitle}`"
              :value="chapter.id"
            />
          </el-select>
        </div>
        <div class="exam-filter-field">
          <span>年份</span>
          <el-input
            v-model.trim="filters.year"
            placeholder="例如 2024"
            @keyup.enter="reloadData"
          />
        </div>
        <div class="exam-filter-field">
          <span>题型</span>
          <el-select
            v-model="filters.questionType"
            clearable
            placeholder="全部题型"
            @change="reloadData"
          >
            <el-option
              v-for="type in questionTypeOptions"
              :key="type"
              :label="type"
              :value="type"
            />
          </el-select>
        </div>
        <button type="button" class="exam-refresh" @click="reloadData">刷新结果</button>
        <button
          type="button"
          class="exam-batch-answer"
          :disabled="batchGenerating"
          @click="handleBatchGenerateAnswers"
        >
          {{ batchGenerating ? '补全中…' : '一键补全答案' }}
        </button>
      </div>
    </section>

    <div class="exam-content">
      <aside class="exam-stats" v-loading="statsLoading">
        <div>
          <p class="eyebrow">knowledge heatmap</p>
          <h3>高频考点</h3>
        </div>
        <span>{{ stats.length }} 项</span>
        <div v-if="stats.length" class="exam-stat-grid">
          <article v-for="stat in stats" :key="stat.knowledgeItemId" class="exam-stat-card">
            <div class="exam-stat-top">
              <strong>{{ stat.knowledgeTitle }}</strong>
              <span>{{ stat.knowledgeItemType || 'UNKNOWN' }}</span>
            </div>
            <p>{{ stat.chapterTitle || '未关联章节' }}</p>
            <div class="exam-stat-meta">
              <span>{{ stat.questionCount }} 题</span>
              <span>{{ stat.totalScore || 0 }} 分</span>
              <span>{{ stat.latestExamYear || '未知年份' }}</span>
            </div>
          </article>
        </div>
        <div v-else class="exam-empty-card">
          <strong>还没有统计结果</strong>
          <p>先完成抽题并建立知识点映射，这里才会出现高频考点。</p>
        </div>
        <div class="exam-trend-panel" v-loading="trendsLoading">
          <div class="exam-trend-heading">
            <strong>趋势分析</strong>
            <span>{{ trends.length }} 项</span>
          </div>
          <div v-if="trends.length" class="exam-trend-list">
            <article v-for="trend in trends.slice(0, 8)" :key="trend.knowledgeItemId" class="exam-trend-card">
              <div class="exam-trend-top">
                <strong>{{ trend.knowledgeTitle }}</strong>
                <span>{{ trend.totalQuestionCount }} 题</span>
              </div>
              <div class="exam-trend-years">
                <div v-for="year in trend.yearlyStats" :key="year.examYear" class="exam-trend-year">
                  <span>{{ year.examYear }}</span>
                  <div>
                    <i :style="{ width: `${Math.max(8, Math.round((year.questionCount / maxTrendQuestionCount) * 100))}%` }"></i>
                  </div>
                  <strong>{{ year.questionCount }}</strong>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="exam-empty-card">
            <strong>暂无趋势数据</strong>
            <p>趋势需要带年份的真题和知识点映射。</p>
          </div>
        </div>
      </aside>

      <section class="exam-questions" v-loading="questionsLoading">
        <div class="exam-section-top">
          <div>
            <p class="eyebrow">question bank</p>
            <h3>课程真题</h3>
          </div>
          <span>{{ questionTotal }} 题</span>
        </div>
        <div v-if="questionTotal" class="exam-question-list">
          <article v-for="question in questions" :key="question.id" class="exam-question-card">
            <div class="exam-question-main">
              <div class="exam-question-top">
                <div>
                  <span>{{ question.questionType || 'UNKNOWN' }}</span>
                </div>
                <small>
                  {{ question.examYear || '未知年份' }}
                  <template v-if="question.sourcePage"> / P{{ question.sourcePage }}</template>
                </small>
              </div>

              <h4>{{ question.materialTitle || '未关联资料' }}</h4>
              <p class="exam-question-text">{{ formatQuestionText(question.questionText) }}</p>
              <p v-if="question.answerText" class="exam-answer-text">答案：{{ question.answerText }}</p>
              <p v-if="question.answerSource" class="exam-answer-source">
                参考：{{ question.answerSource }}
                <template v-if="question.answerSourcePage"> 第 {{ question.answerSourcePage }} 页</template>
              </p>

              <div class="exam-question-meta">
                <span>{{ question.chapterTitle || '未关联章节' }}</span>
                <span>{{ question.score || 0 }} 分</span>
                <span>{{ question.difficultyLevel || '难度未知' }}</span>
              </div>
            </div>

            <div class="exam-question-side">
              <div class="exam-mapping-block">
                <strong>已映射知识点</strong>
                <div class="exam-mapping-list">
                  <span
                    v-for="mapping in question.mappings"
                    :key="mapping.id || `${question.id}-${mapping.knowledgeItemId}`"
                    class="exam-mapping-tag"
                  >
                    {{ mapping.knowledgeTitle }}
                  </span>
                  <span v-if="!question.mappings?.length" class="exam-mapping-empty">尚未映射知识点</span>
                </div>
              </div>

              <div class="exam-mapping-editor">
                <el-select
                  v-model="mappingDrafts[question.id]"
                  clearable
                  filterable
                  placeholder="选择一个知识点"
                >
                  <el-option
                    v-for="item in knowledgeItems"
                    :key="item.id"
                    :label="item.title"
                    :value="item.id"
                  />
                </el-select>
                <button
                  type="button"
                  :disabled="mappingSavingQuestionId === question.id"
                  @click="submitMapping(question)"
                >
                  {{ mappingSavingQuestionId === question.id ? '保存中…' : '建立映射' }}
                </button>
              </div>

              <div class="exam-generate-answer">
                <button
                  type="button"
                  :disabled="generatingAnswerQuestionId === question.id"
                  @click="handleGenerateAnswer(question)"
                >
                  {{ generatingAnswerQuestionId === question.id ? '生成中…' : (question.answerText ? '重新生成' : '生成答案') }}
                </button>
              </div>
            </div>
          </article>
        </div>
        <div v-if="totalQuestionPages > 1" class="exam-pagination">
          <span>第 {{ questionPage }} / {{ totalQuestionPages }} 页</span>
          <div class="exam-pagination-actions">
            <button type="button" :disabled="questionPage === 1" @click="questionPage -= 1">上一页</button>
            <button type="button" :disabled="questionPage === totalQuestionPages" @click="questionPage += 1">
              下一页
            </button>
          </div>
        </div>
        <div v-if="!questionTotal" class="exam-empty-card">
          <strong>暂未选择真题资料</strong>
          <p>请点击上方真题资料卡片选择要查看的试卷，或先抽取题目。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parsePdf } from '../../api/material'
import {
  extractExamQuestions,
  generateAnswer,
  generateBatchAnswers,
  listExamKnowledgeStats,
  listExamKnowledgeTrends,
  listExamQuestions,
  saveExamKnowledgeMap
} from '../../api/exam'

const props = defineProps({
  selectedCourse: {
    type: Object,
    default: null
  },
  currentUserId: {
    type: Number,
    default: null
  },
  materials: {
    type: Array,
    default: () => []
  },
  chapters: {
    type: Array,
    default: () => []
  },
  knowledgeItems: {
    type: Array,
    default: () => []
  },
  preferredMaterialId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['material-parsed', 'stats-changed'])

const questions = ref([])
const stats = ref([])
const trends = ref([])
const questionsLoading = ref(false)
const statsLoading = ref(false)
const trendsLoading = ref(false)
const extractingMaterialIds = reactive(new Set())
const mappingSavingQuestionId = ref(null)
const generatingAnswerQuestionId = ref(null)
const batchGenerating = ref(false)
const questionPage = ref(1)
const questionTotal = ref(0)
const totalQuestionPages = ref(0)
const mappingDrafts = reactive({})
const selectedMaterialIds = ref([])
const filters = reactive({
  year: '',
  chapterIds: [],
  questionType: null
})

const questionTypeOptions = ['单选题', '多选题', '填空题', '简答题', '编程题', '设计题', '其他']
const MATERIAL_NOT_PARSED = 'Material must be parsed before exam extraction'
const EXTRACTION_EXISTS = 'Exam questions already exist for this material'
const PDF_ONLY = 'Only PDF exam materials support extraction'

const examMaterials = computed(() => props.materials.filter(item => item.materialType === 'EXAM'))
const maxTrendQuestionCount = computed(() => Math.max(
  1,
  ...trends.value.flatMap(trend => (
    trend.yearlyStats || []
  ).map(year => year.questionCount || 0))
))
const questionPageSize = 12
const lastQuestionFilterKey = ref('')

watch(
  () => props.selectedCourse?.id,
  async courseId => {
    resetState()
    if (courseId) {
      await reloadData()
    }
  },
  { immediate: true }
)

watch(
  () => props.preferredMaterialId,
  materialId => {
    if (!materialId) return
    if (!selectedMaterialIds.value.includes(materialId)) {
      selectedMaterialIds.value.push(materialId)
    }
    if (props.selectedCourse?.id) {
      reloadData()
    }
  }
)

watch(
  () => props.materials.length,
  (current, previous) => {
    if (current < previous && props.selectedCourse?.id) {
      reloadData()
    }
  }
)

watch(questionPage, (page, previousPage) => {
  if (page === previousPage || !props.selectedCourse?.id) return
  reloadData()
})

async function reloadData() {
  if (!props.selectedCourse?.id || !props.currentUserId) return
  const filterKey = JSON.stringify(normalizedFilters(true))
  if (filterKey !== lastQuestionFilterKey.value) {
    lastQuestionFilterKey.value = filterKey
    if (questionPage.value !== 1) {
      questionPage.value = 1
      return
    }
  }
  questionsLoading.value = true
  statsLoading.value = true
  trendsLoading.value = true
  try {
    if (normalizedFilters(true).materialIds && normalizedFilters(true).materialIds.length) {
      const questionData = await listExamQuestions(props.selectedCourse.id, props.currentUserId, {
        ...normalizedFilters(true),
        page: questionPage.value,
        size: questionPageSize
      })
      questions.value = questionData.items || []
      questionTotal.value = questionData.total || 0
      totalQuestionPages.value = questionData.totalPages || 0
      questionPage.value = questionData.page || 1
    } else {
      questions.value = []
      questionTotal.value = 0
      totalQuestionPages.value = 0
    }
    const [statData, trendData] = await Promise.all([
      listExamKnowledgeStats(props.selectedCourse.id, props.currentUserId, normalizedFilters(false)),
      listExamKnowledgeTrends(props.selectedCourse.id, props.currentUserId, normalizedFilters(false))
    ])
    stats.value = statData
    trends.value = trendData
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    questionsLoading.value = false
    statsLoading.value = false
    trendsLoading.value = false
  }
}

async function handleExtract(material) {
  if (!props.currentUserId) return
  const supported = ['pdf', 'doc', 'docx', 'md', 'txt']
  if (!supported.includes(material.fileType?.toLowerCase())) {
    ElMessage.warning('不支持的真题文件格式；支持 PDF、DOC、DOCX、MD、TXT')
    return
  }

  if (!material.parsedChunkCount) {
    const parsed = await confirmParse(material)
    if (!parsed) return
  }

  let overwrite = false
  if (questions.value.some(question => question.materialId === material.id)) {
    overwrite = await confirmOverwrite()
    if (!overwrite) return
  }

  await doExtract(material, overwrite)
}

async function confirmParse(material) {
  try {
    await ElMessageBox.confirm(
      '这份资料还没有解析文本，先解析资料再继续抽题？',
      '先解析资料',
      {
        confirmButtonText: '去解析资料',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return false
  }

  extractingMaterialIds.add(material.id)
  try {
    const result = await parsePdf(material.id, props.currentUserId)
    emit('material-parsed', {
      materialId: material.id,
      chunkCount: result.chunkCount
    })
    emit('stats-changed')
    material.parsedChunkCount = result.chunkCount
    ElMessage.success(`解析完成，共提取 ${result.chunkCount} 个文本块`)
    return true
  } catch (error) {
    ElMessage.error(error.message)
    return false
  } finally {
    extractingMaterialIds.delete(material.id)
  }
}

async function confirmOverwrite() {
  try {
    await ElMessageBox.confirm(
      '覆盖后会清空该资料已有的题目映射和高频统计，是否继续？',
      '覆盖重写',
      {
        confirmButtonText: '覆盖重写',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    return true
  } catch {
    return false
  }
}

async function doExtract(material, overwrite) {
  extractingMaterialIds.add(material.id)
  try {
    const result = await extractExamQuestions(material.id, props.currentUserId, overwrite)
    if (!selectedMaterialIds.value.includes(material.id)) {
      selectedMaterialIds.value.push(material.id)
    }
    material.questionCount = result.length
    await reloadData()
    emit('stats-changed')
    ElMessage.success(`已抽取 ${result.length} 道题目`)
  } catch (error) {
    if (error.message === MATERIAL_NOT_PARSED) {
      const parsed = await confirmParse(material)
      if (parsed) {
        await doExtract(material, overwrite)
      }
      return
    }
    if (error.message === EXTRACTION_EXISTS) {
      const confirmed = await confirmOverwrite()
      if (confirmed) {
        await doExtract(material, true)
      }
      return
    }
    if (error.message === PDF_ONLY || error.code === 'EXAM_PDF_ONLY') {
      ElMessage.warning('不支持的真题文件格式；支持 PDF、DOC、DOCX、MD、TXT')
      return
    }
    ElMessage.error(error.message)
  } finally {
    extractingMaterialIds.delete(material.id)
  }
}

async function submitMapping(question) {
  const knowledgeItemId = mappingDrafts[question.id]
  if (!knowledgeItemId) {
    ElMessage.warning('请先选择一个知识点')
    return
  }

  mappingSavingQuestionId.value = question.id
  try {
    await saveExamKnowledgeMap(question.id, props.currentUserId, {
      knowledgeItemId,
      matchSource: 'MANUAL'
    })
    mappingDrafts[question.id] = null
    await reloadData()
    emit('stats-changed')
    ElMessage.success('题目与知识点已建立映射')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    mappingSavingQuestionId.value = null
  }
}

function formatQuestionText(text) {
  if (!text) return ''
  return text
    .replace(/([^\n])([A-Ea-e][\.\)、．])\s*(?=\S)/g, '$1\n$2')
    .replace(/([^\n])([①-⑧])/g, '$1\n$2')
    .replace(/^\n/, '')
}

async function handleGenerateAnswer(question) {
  if (!props.currentUserId) return
  generatingAnswerQuestionId.value = question.id
  try {
    const result = await generateAnswer(question.id, props.currentUserId)
    question.answerText = result.answerText
    question.answerSource = result.answerSource
    question.answerSourcePage = result.answerSourcePage
    ElMessage.success('答案已生成')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    generatingAnswerQuestionId.value = null
  }
}

async function handleBatchGenerateAnswers() {
  if (!props.currentUserId || !props.selectedCourse?.id) return
  batchGenerating.value = true
  try {
    const result = await generateBatchAnswers(props.selectedCourse.id, props.currentUserId)
    ElMessage.success(`已处理 ${result.total} 题，成功 ${result.success}，失败 ${result.failed}`)
    await reloadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    batchGenerating.value = false
  }
}

function toggleMaterial(materialId) {
  const index = selectedMaterialIds.value.indexOf(materialId)
  if (index >= 0) {
    selectedMaterialIds.value.splice(index, 1)
  } else {
    selectedMaterialIds.value.push(materialId)
  }
  reloadData()
}

function normalizedFilters(includeMaterialIds) {
  return {
    year: filters.year ? Number(filters.year) : undefined,
    chapterIds: filters.chapterIds.length ? filters.chapterIds : undefined,
    questionType: filters.questionType || undefined,
    materialIds: includeMaterialIds
      ? selectedMaterialIds.value.slice()
      : undefined
  }
}

function resetState() {
  questions.value = []
  questionTotal.value = 0
  totalQuestionPages.value = 0
  questionPage.value = 1
  stats.value = []
  trends.value = []
  filters.year = ''
  filters.chapterIds = []
  filters.questionType = null
  selectedMaterialIds.value = []
  lastQuestionFilterKey.value = ''
  Object.keys(mappingDrafts).forEach(key => {
    delete mappingDrafts[key]
  })
}
</script>

<style scoped>
.exam-panel {
  min-height: 100%;
  display: grid;
  gap: 34px;
  padding: 72px clamp(38px, 6vw, 90px) 96px;
  background: #f5f3ef;
}

.exam-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.exam-heading h2 span {
  color: #ff3151;
}

.exam-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.exam-heading > p {
  margin: 0 0 6px;
  color: #3f4352;
  font-size: 16px;
  line-height: 1.7;
}

.exam-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.exam-overview-card {
  padding: 22px 24px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #ff3151;
}

.exam-overview-card strong,
.exam-overview-card span {
  display: block;
}

.exam-overview-card strong {
  font-size: 40px;
  letter-spacing: -0.06em;
}

.exam-overview-card span {
  margin-top: 6px;
  color: #666;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.exam-control-panel,
.exam-stats,
.exam-questions,
.exam-empty-card {
  border: 1px solid #111;
  background: #fff;
}

.exam-control-panel {
  padding: 24px;
  box-shadow: 10px 10px 0 #14cbea;
}

.exam-toolbar {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) 148px 148px;
  gap: 12px;
  align-items: end;
  margin-top: 28px;
}

.exam-filter-field {
  min-width: 0;
}

.exam-filter-field span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

:deep(.exam-filter-field .el-select),
:deep(.exam-filter-field .el-input) {
  width: 100%;
}

:deep(.exam-filter-field .el-select__wrapper),
:deep(.exam-filter-field .el-input__wrapper),
:deep(.exam-mapping-editor .el-select__wrapper) {
  min-height: 52px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.exam-refresh,
.exam-batch-answer {
  min-height: 52px;
  border: 1px solid #111;
  width: 148px;
  font-weight: 700;
}

.exam-refresh {
  background: #111;
  color: #fff;
}

.exam-batch-answer {
  background: #ffef5a;
  color: #111;
}

.exam-strip-heading,
.exam-section-top {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
}

.exam-strip-heading h3,
.exam-section-top h3 {
  margin: 8px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.exam-strip-heading > span,
.exam-section-top > span {
  color: #666;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.exam-materials {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.exam-material-card,
.exam-stat-card,
.exam-question-card {
  border: 1px solid #111;
  background: #fff;
}

.exam-material-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  box-shadow: 8px 8px 0 #111;
  cursor: pointer;
}

.exam-material-card.active {
  box-shadow: 8px 8px 0 #ff3151;
}

.exam-material-copy strong,
.exam-stat-card strong,
.exam-question-top strong,
.exam-mapping-block strong {
  display: block;
}

.exam-material-copy small,
.exam-question-card small,
.exam-stat-card p {
  color: #666;
}

.exam-material-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.exam-material-meta span,
.exam-question-top span,
.exam-stat-top span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid #111;
  background: #f5f3ef;
  font-size: 12px;
}

.exam-material-card button,
.exam-mapping-editor button {
  min-height: 48px;
  border: 1px solid #111;
  background: #ffef5a;
  font-weight: 700;
}

.exam-material-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.exam-content {
  display: grid;
  grid-template-columns: minmax(280px, 0.38fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.exam-stats,
.exam-questions {
  min-height: 240px;
  padding: 24px;
}

.exam-stat-grid {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.exam-stat-card {
  padding: 16px;
  box-shadow: inset 0 5px 0 #14cbea;
}

.exam-stat-top,
.exam-question-top,
.exam-question-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.exam-stat-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
  font-size: 13px;
}

.exam-trend-panel {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #ddd;
}

.exam-trend-heading,
.exam-trend-top,
.exam-trend-year {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.exam-trend-heading strong {
  font-size: 20px;
}

.exam-trend-heading span,
.exam-trend-top span,
.exam-trend-year span,
.exam-trend-year strong {
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.exam-trend-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.exam-trend-card {
  padding: 14px;
  border: 1px solid #111;
  background: #fff;
}

.exam-trend-top strong {
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exam-trend-years {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.exam-trend-year {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 24px;
}

.exam-trend-year div {
  height: 8px;
  overflow: hidden;
  border: 1px solid #111;
  background: #f5f3ef;
}

.exam-trend-year i {
  display: block;
  height: 100%;
  background: #ff3151;
}

.exam-question-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.exam-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #ddd;
  color: #666;
  font-size: 13px;
}

.exam-pagination-actions {
  display: flex;
  gap: 10px;
}

.exam-pagination-actions button {
  min-height: 42px;
  min-width: 92px;
  border: 1px solid #111;
  background: #fff;
  font-weight: 700;
}

.exam-question-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.34fr);
  gap: 22px;
  padding: 22px;
  box-shadow: 10px 10px 0 #f6d743;
}

.exam-question-card h4 {
  margin: 14px 0 0;
  font-size: 22px;
  letter-spacing: -0.03em;
}

.exam-question-text {
  margin: 14px 0 10px;
  white-space: pre-wrap;
  line-height: 1.7;
}

.exam-answer-text {
  margin: 0 0 6px;
  padding: 10px 12px;
  background: #f5f3ef;
}

.exam-answer-source {
  margin: 0 0 12px;
  color: #777;
  font-size: 13px;
  line-height: 1.6;
}

.exam-question-meta {
  flex-wrap: wrap;
  font-size: 13px;
  color: #666;
}

.exam-question-side {
  display: grid;
  gap: 14px;
  align-content: start;
}

.exam-mapping-block {
  padding: 16px;
  border: 1px solid #111;
  background: #f9faf6;
}

.exam-mapping-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.exam-mapping-tag,
.exam-mapping-empty {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid #111;
  font-size: 13px;
}

.exam-mapping-tag {
  background: #dff8f3;
}

.exam-mapping-empty {
  background: #f1f1f1;
  color: #6a6f81;
}

.exam-mapping-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
}

.exam-mapping-editor :deep(.el-select) {
  width: 100%;
}

.exam-generate-answer {
  margin-top: 4px;
}

.exam-generate-answer button {
  width: 100%;
  min-height: 44px;
  border: 1px solid #111;
  background: #14cbea;
  color: #111;
  font-weight: 700;
}

.exam-empty-card {
  padding: 22px;
  box-shadow: 8px 8px 0 #111;
}

@media (max-width: 900px) {
  .exam-heading,
  .exam-content {
    grid-template-columns: 1fr;
  }

  .exam-overview {
    grid-template-columns: 1fr;
  }

  .exam-toolbar {
    grid-template-columns: 1fr 1fr 1fr;
  }

  .exam-refresh,
  .exam-batch-answer {
    width: 100%;
  }

  .exam-heading > p {
    margin-top: 18px;
  }

  .exam-refresh {
    width: 100%;
  }

  .exam-question-card {
    grid-template-columns: 1fr;
  }

  .exam-pagination {
    display: block;
  }

  .exam-pagination-actions {
    margin-top: 12px;
  }
}

@media (max-width: 760px) {
  .exam-panel {
    padding: 58px 22px 88px;
  }

  .exam-heading,
  .exam-strip-heading,
  .exam-section-top {
    display: block;
  }

  .exam-heading > p,
  .exam-strip-heading > span,
  .exam-section-top > span {
    margin-top: 18px;
  }

  .exam-toolbar {
    grid-template-columns: 1fr;
  }

  .exam-refresh,
  .exam-batch-answer {
    width: 100%;
  }

  .exam-mapping-editor {
    grid-template-columns: 1fr;
  }

  .exam-material-actions {
    grid-template-columns: 1fr;
  }

  .exam-question-top,
  .exam-question-meta {
    display: block;
  }

  .exam-question-top small {
    display: block;
    margin-top: 10px;
  }
}
</style>
