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
      <div class="exam-toolbar">
        <div class="exam-filter-field">
          <span>资料</span>
          <el-select
            v-model="filters.materialId"
            clearable
            placeholder="全部真题资料"
            @change="reloadData"
          >
            <el-option
              v-for="material in examMaterials"
              :key="material.id"
              :label="material.title"
              :value="material.id"
            />
          </el-select>
        </div>
        <div class="exam-filter-field">
          <span>章节</span>
          <el-select
            v-model="filters.chapterId"
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
      </div>

      <div class="exam-material-strip">
        <div class="exam-strip-heading">
          <div>
            <p class="eyebrow">exam materials</p>
            <h3>选择真题资料并触发抽题</h3>
          </div>
          <span>{{ examMaterials.length }} 份</span>
        </div>
        <div v-if="selectedExamMaterial" class="exam-selected-card">
          <div class="exam-selected-copy">
            <p class="exam-selected-kicker">当前聚焦资料</p>
            <strong>{{ selectedExamMaterial.title }}</strong>
            <p>{{ selectedExamMaterial.year || '未标年份' }} / {{ selectedExamMaterial.originalName }}</p>
          </div>
          <div class="exam-selected-meta">
            <span>{{ (selectedExamMaterial.fileType || 'file').toUpperCase() }}</span>
            <span>{{ selectedExamMaterial.parsedChunkCount > 0 ? '已解析' : '未解析' }}</span>
            <button
              type="button"
              :disabled="extractingMaterialId === selectedExamMaterial.id"
              @click="handleExtract(selectedExamMaterial)"
            >
              {{ extractingMaterialId === selectedExamMaterial.id ? '抽取中…' : '抽取题目' }}
            </button>
          </div>
        </div>
        <div class="exam-materials">
          <article
            v-for="material in examMaterials"
            :key="material.id"
            class="exam-material-card"
            :class="{ active: Number(filters.materialId) === material.id }"
          >
            <div class="exam-material-copy">
              <strong>{{ material.title }}</strong>
              <small>{{ material.year || '未标年份' }} / {{ material.originalName }}</small>
            </div>
            <div class="exam-material-meta">
              <span>{{ (material.fileType || 'file').toUpperCase() }}</span>
              <span>{{ material.parsedChunkCount > 0 ? '已解析' : '未解析' }}</span>
            </div>
            <div class="exam-material-actions">
              <button type="button" class="exam-focus-button" @click="focusMaterial(material.id)">
                {{ Number(filters.materialId) === material.id ? '当前筛选中' : '查看本资料题目' }}
              </button>
              <button
                type="button"
                :disabled="extractingMaterialId === material.id"
                @click="handleExtract(material)"
              >
                {{ extractingMaterialId === material.id ? '抽取中…' : '抽取题目' }}
              </button>
            </div>
          </article>
          <div v-if="examMaterials.length === 0" class="exam-empty-card">
            <strong>还没有 EXAM 类型资料</strong>
            <p>请先上传往年真题资料，再进行题目抽取。</p>
          </div>
        </div>
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
              <p class="exam-question-text">{{ question.questionText }}</p>
              <p v-if="question.answerText" class="exam-answer-text">答案：{{ question.answerText }}</p>

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
          <strong>当前还没有真题题目</strong>
          <p>从上方 EXAM 资料中抽取题目后，这里会显示题目列表。</p>
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
  listExamKnowledgeStats,
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

const emit = defineEmits(['material-parsed'])

const questions = ref([])
const stats = ref([])
const questionsLoading = ref(false)
const statsLoading = ref(false)
const extractingMaterialId = ref(null)
const mappingSavingQuestionId = ref(null)
const questionPage = ref(1)
const questionTotal = ref(0)
const totalQuestionPages = ref(0)
const mappingDrafts = reactive({})
const filters = reactive({
  year: '',
  chapterId: null,
  questionType: null,
  materialId: null
})

const questionTypeOptions = ['单选题', '多选题', '名词解释', '简答题', '编程题', '设计题', '其他']
const MATERIAL_NOT_PARSED = 'Material must be parsed before exam extraction'
const EXTRACTION_EXISTS = 'Exam questions already exist for this material'
const PDF_ONLY = 'Only PDF exam materials support extraction'

const examMaterials = computed(() => props.materials.filter(item => item.materialType === 'EXAM'))
const selectedExamMaterial = computed(() => {
  if (!examMaterials.value.length) return null
  return examMaterials.value.find(item => item.id === Number(filters.materialId)) || examMaterials.value[0]
})
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
    filters.materialId = materialId
    if (props.selectedCourse?.id) {
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
  try {
    const [questionData, statData] = await Promise.all([
      listExamQuestions(props.selectedCourse.id, props.currentUserId, {
        ...normalizedFilters(true),
        page: questionPage.value,
        size: questionPageSize
      }),
      listExamKnowledgeStats(props.selectedCourse.id, props.currentUserId, normalizedFilters(false))
    ])
    questions.value = questionData.items || []
    questionTotal.value = questionData.total || 0
    totalQuestionPages.value = questionData.totalPages || 0
    questionPage.value = questionData.page || 1
    stats.value = statData
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    questionsLoading.value = false
    statsLoading.value = false
  }
}

async function handleExtract(material) {
  if (!props.currentUserId) return
  if (material.fileType?.toLowerCase() !== 'pdf') {
    ElMessage.warning('当前 EXAM 抽题主流程仅支持 PDF 资料')
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

  extractingMaterialId.value = material.id
  try {
    const result = await parsePdf(material.id, props.currentUserId)
    emit('material-parsed', {
      materialId: material.id,
      chunkCount: result.chunkCount
    })
    material.parsedChunkCount = result.chunkCount
    ElMessage.success(`解析完成，共提取 ${result.chunkCount} 个文本块`)
    return true
  } catch (error) {
    ElMessage.error(error.message)
    return false
  } finally {
    extractingMaterialId.value = null
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
  extractingMaterialId.value = material.id
  try {
    const result = await extractExamQuestions(material.id, props.currentUserId, overwrite)
    filters.materialId = material.id
    await reloadData()
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
      ElMessage.warning('当前 EXAM 抽题主流程仅支持 PDF 资料')
      return
    }
    ElMessage.error(error.message)
  } finally {
    extractingMaterialId.value = null
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
    ElMessage.success('题目与知识点已建立映射')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    mappingSavingQuestionId.value = null
  }
}

function focusMaterial(materialId) {
  if (Number(filters.materialId) === materialId) return
  filters.materialId = materialId
  reloadData()
}

function normalizedFilters(includeMaterialId) {
  return {
    year: filters.year ? Number(filters.year) : undefined,
    chapterId: filters.chapterId || undefined,
    questionType: filters.questionType || undefined,
    materialId: includeMaterialId ? filters.materialId || undefined : undefined
  }
}

function resetState() {
  questions.value = []
  questionTotal.value = 0
  totalQuestionPages.value = 0
  questionPage.value = 1
  stats.value = []
  filters.year = ''
  filters.chapterId = null
  filters.questionType = null
  filters.materialId = props.preferredMaterialId || null
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
  grid-template-columns: repeat(4, minmax(0, 1fr)) 148px;
  gap: 12px;
  align-items: end;
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

.exam-refresh {
  min-height: 52px;
  border: 1px solid #111;
  background: #111;
  width: 148px;
  color: #111;
  font-weight: 700;
  color: #fff;
}

.exam-material-strip {
  margin-top: 28px;
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

.exam-selected-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: end;
  margin-top: 18px;
  padding: 20px 22px;
  border: 1px solid #111;
  background: #fff7df;
  box-shadow: 8px 8px 0 #ff3151;
}

.exam-selected-kicker {
  margin: 0 0 10px;
  color: #666;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.exam-selected-copy strong {
  display: block;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.exam-selected-copy p:last-child {
  margin: 10px 0 0;
  color: #555;
  line-height: 1.6;
}

.exam-selected-meta {
  display: grid;
  justify-items: end;
  gap: 10px;
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
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.exam-focus-button {
  background: #fff;
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
  margin: 0 0 12px;
  padding: 10px 12px;
  background: #f5f3ef;
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
    grid-template-columns: 1fr 1fr;
  }

  .exam-selected-card {
    grid-template-columns: 1fr;
  }

  .exam-selected-meta {
    justify-items: start;
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

  .exam-toolbar,
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
