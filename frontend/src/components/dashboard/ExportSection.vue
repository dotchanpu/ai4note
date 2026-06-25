<template>
<section
            id="export"
            class="export-section scroll-panel"
            :class="{ 'section-active': active }"
          >
            <div class="export-heading">
              <div>
                <p class="eyebrow">知识包导出</p>
                <h2>导出给 AI 使用的课程知识包<span>.</span></h2>
              </div>
              <p>把课程资料、标签、知识条目、章节结构和考点统计整理成 ZIP，保存导出记录并随时下载。</p>
            </div>

            <div class="export-panel">
              <div class="export-form-grid">
                <label class="export-field">
                  <span>导出名称</span>
                  <el-input v-model="exportForm.exportName" maxlength="255" placeholder="例如：期末复习知识包" />
                </label>
                <label class="export-field">
                  <span>导出模板</span>
                  <el-select v-model="exportForm.templateId" placeholder="选择模板">
                    <el-option
                      v-for="template in exportTemplates"
                      :key="template.id"
                      :label="`${template.templateName} / ${template.targetAgent}`"
                      :value="template.id"
                    />
                  </el-select>
                </label>
                <label class="export-field">
                  <span>章节范围</span>
                  <el-select v-model="exportForm.chapterIds" multiple collapse-tags clearable placeholder="全部章节">
                    <el-option
                      v-for="chapter in chapters"
                      :key="chapter.id"
                      :label="`${chapter.chapterNo} ${chapter.chapterTitle}`"
                      :value="chapter.id"
                    />
                  </el-select>
                </label>
                <label class="export-field">
                  <span>资料类型</span>
                  <el-select v-model="exportForm.materialTypes" multiple collapse-tags clearable placeholder="全部类型">
                    <el-option
                      v-for="option in materialTypeOptions"
                      :key="option.type"
                      :label="option.label"
                      :value="option.type"
                    />
                  </el-select>
                </label>
              </div>

              <div class="export-options">
                <label>
                  <el-switch v-model="exportForm.onlyKeyMaterials" />
                  <span>只导出重点资料</span>
                </label>
                <label>
                  <el-switch v-model="exportForm.includeExamStats" />
                  <span>包含高频考点统计</span>
                </label>
                <label
                  v-for="option in exportRelationOptions"
                  :key="option.type"
                  :class="{ 'option-disabled': option.count === 0 }"
                >
                  <el-switch
                    v-model="exportForm[option.exportField]"
                    :disabled="option.count === 0"
                  />
                  <span>包含{{ option.label }}重点内容</span>
                  <small>{{ option.count }} 门</small>
                </label>
                <button
                  type="button"
                  class="export-preview-button"
                  :disabled="exportPreviewLoading || exportCreating"
                  @click="runExportPreview"
                >
                  <span>{{ exportPreviewLoading ? '预览中…' : '预览内容' }}</span>
                  <strong>?</strong>
                </button>
                <button type="button" :disabled="exportCreating" @click="runExport">
                  <span>{{ exportCreating ? '导出中…' : '生成知识包' }}</span>
                  <strong>→</strong>
                </button>
              </div>

              <div v-loading="exportPreviewLoading" class="export-preview">
                <template v-if="exportPreview">
                  <div class="export-preview-heading">
                    <div>
                      <span>预览范围</span>
                      <strong>{{ exportPreview.courseName }}</strong>
                    </div>
                    <p>{{ exportPreview.templateName || '默认模板' }} · {{ exportPreview.exportFormat }}</p>
                  </div>

                  <div class="export-preview-stats">
                    <div>
                      <span>章节</span>
                      <strong>{{ exportPreview.summary.chapterCount }}</strong>
                    </div>
                    <div>
                      <span>资料</span>
                      <strong>{{ exportPreview.summary.materialCount }}</strong>
                    </div>
                    <div>
                      <span>已解析</span>
                      <strong>{{ exportPreview.summary.parsedMaterialCount }}</strong>
                    </div>
                    <div>
                      <span>知识条目</span>
                      <strong>{{ exportPreview.summary.knowledgeItemCount }}</strong>
                    </div>
                    <div>
                      <span>考点统计</span>
                      <strong>{{ exportPreview.summary.examStatCount }}</strong>
                    </div>
                    <div>
                      <span>关联课程</span>
                      <strong>{{ exportPreview.summary.relatedCourseCount }}</strong>
                    </div>
                  </div>

                  <div class="export-preview-grid">
                    <section>
                      <h3>课程章节</h3>
                      <ul>
                        <li v-for="chapter in exportPreview.chapters" :key="chapter.id">
                          {{ chapter.chapterNo }} {{ chapter.chapterTitle }}
                        </li>
                      </ul>
                      <p v-if="exportPreview.chapters.length === 0">未包含章节结构。</p>
                    </section>

                    <section>
                      <h3>课程资料</h3>
                      <ul>
                        <li v-for="material in exportPreview.materials" :key="material.id">
                          <strong>{{ material.title }}</strong>
                          <span>{{ materialTypeLabel(material.materialType) }} · {{ material.parsedChunkCount }} 块文本</span>
                        </li>
                      </ul>
                      <p v-if="exportPreview.materials.length === 0">当前筛选条件下没有资料。</p>
                    </section>

                    <section>
                      <h3>知识条目</h3>
                      <ul>
                        <li v-for="item in exportPreview.knowledgeItems" :key="item.id">
                          <strong>{{ item.title }}</strong>
                          <span>{{ knowledgeTypeLabel(item.itemType) }} · 重要度 {{ item.importanceLevel || '-' }}</span>
                        </li>
                      </ul>
                      <p v-if="exportPreview.knowledgeItems.length === 0">当前筛选条件下没有知识条目。</p>
                    </section>

                    <section>
                      <h3>考点统计</h3>
                      <ul>
                        <li v-for="stat in exportPreview.examStats" :key="stat.knowledgeItemId || stat.knowledgeTitle">
                          <strong>{{ stat.knowledgeTitle }}</strong>
                          <span>{{ stat.questionCount }} 题 · {{ stat.totalScore || 0 }} 分 · {{ stat.latestExamYear || '未知年份' }}</span>
                        </li>
                      </ul>
                      <p v-if="exportPreview.examStats.length === 0">未包含考点统计。</p>
                    </section>
                  </div>

                  <div class="export-preview-related">
                    <div class="export-preview-related-title">
                      <strong>{{ exportPreview.summary.relatedMaterialCount }}</strong>
                      <span>关联课程重点资料</span>
                      <strong>{{ exportPreview.summary.relatedKnowledgeItemCount }}</strong>
                      <span>关联课程知识条目</span>
                    </div>
                    <article v-for="course in exportPreview.relatedCourses" :key="course.courseId">
                      <div>
                        <span>{{ relationTypeLabel(course.relationType) }}</span>
                        <h3>{{ course.courseName }}</h3>
                        <p>{{ course.chapterCount }} 个章节 · {{ course.materials.length }} 份重点资料 · {{ course.knowledgeItems.length }} 条知识</p>
                      </div>
                      <ul>
                        <li v-for="material in course.materials" :key="material.id">{{ material.title }}</li>
                      </ul>
                    </article>
                    <p v-if="exportPreview.relatedCourses.length === 0">未包含关联课程内容。</p>
                  </div>
                </template>
                <div v-else class="export-preview-empty">
                  <strong>先预览，再导出。</strong>
                  <p>预览会按当前筛选条件展示 ZIP 将包含的课程结构、资料、知识、考点统计和关联课程内容。</p>
                </div>
              </div>
            </div>

            <div class="export-records">
              <div class="export-record-heading">
                <div>
                  <strong>{{ exportRecords.length }}</strong>
                  <span>导出记录</span>
                </div>
                <button type="button" :disabled="exportLoading" @click="loadExportData">刷新记录</button>
              </div>

              <div v-loading="exportLoading" class="export-record-grid">
                <article v-for="record in exportRecords" :key="record.id" class="export-record-card">
                  <div>
                    <div class="export-record-tags">
                      <span>v{{ record.versionNo || 1 }}</span>
                      <span>{{ record.exportFormat }}</span>
                      <span v-if="record.recommended" class="recommended-version">推荐版本</span>
                    </div>
                    <h3>{{ record.exportName }}</h3>
                    <p>{{ formatExportTime(record.exportTime) }}</p>
                  </div>
                  <div class="export-record-actions">
                    <button type="button" @click="downloadExport(record)">下载 ZIP</button>
                    <button
                      type="button"
                      :disabled="record.recommended"
                      @click="setRecommendedExport(record)"
                    >
                      {{ record.recommended ? '当前推荐' : '设为推荐' }}
                    </button>
                  </div>
                </article>
                <div v-if="!exportLoading && exportRecords.length === 0" class="export-empty">
                  <strong>还没有导出记录。</strong>
                  <p>生成第一个课程知识包后，这里会保存记录和下载入口。</p>
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
  exportLoading,
  exportCreating,
  exportPreviewLoading,
  chapters,
  materials,
  knowledgeItems,
  exportTemplates,
  exportRecords,
  exportPreview,
  materialTypeOptions,
  exportRelationOptions,
  exportForm,
  loadExportData,
  runExport,
  runExportPreview,
  downloadExport,
  setRecommendedExport,
  formatExportTime,
  knowledgeTypeLabel,
  materialTypeLabel,
  relationTypeLabel
} = useDashboardContext()
</script>
