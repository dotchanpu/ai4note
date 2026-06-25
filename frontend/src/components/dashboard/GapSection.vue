<template>
<section
            id="gaps"
            class="gap-section scroll-panel"
            :class="{ 'section-active': active }"
          >
            <div class="gap-heading">
              <div>
                <p class="eyebrow">知识缺口</p>
                <h2>找出复习优先级<span>.</span></h2>
              </div>
              <p>结合课程知识点、前置课程、掌握状态和真题高频考点，生成可追溯的薄弱知识清单。</p>
            </div>

            <div class="gap-generator">
              <label>
                <span>报告名称</span>
                <el-input v-model="gapForm.reportName" maxlength="128" placeholder="例如：期末前知识缺口检查" />
              </label>
              <label class="gap-switch">
                <el-switch v-model="gapForm.includePrerequisites" />
                <span>包含前置课程</span>
                <small>{{ relationGroups[0]?.items.length || 0 }} 门</small>
              </label>
              <button type="button" :disabled="gapGenerating" @click="generateGapReport">
                {{ gapGenerating ? '生成中…' : '生成报告' }}
              </button>
            </div>

            <div v-loading="prerequisiteGapLoading" class="prerequisite-hint-panel">
              <div class="prerequisite-hint-heading">
                <div>
                  <span>前置课程提示</span>
                  <strong>{{ prerequisiteGapHints.length }}</strong>
                </div>
                <button type="button" :disabled="prerequisiteGapLoading" @click="loadPrerequisiteGapHints">
                  {{ prerequisiteGapLoading ? '检查中…' : '检查前置缺口' }}
                </button>
              </div>
              <div v-if="prerequisiteGapHints.length > 0" class="prerequisite-hint-list">
                <article
                  v-for="item in prerequisiteGapHints.slice(0, 6)"
                  :key="`${item.sourceCourseId}-${item.knowledgeItemId}`"
                >
                  <div>
                    <span>{{ item.sourceCourseName }}</span>
                    <strong>优先级 {{ item.severityLevel }}</strong>
                  </div>
                  <h3>{{ item.knowledgeTitle }}</h3>
                  <p>{{ item.reason }}</p>
                  <small>{{ item.suggestion }}</small>
                </article>
              </div>
              <div v-else class="prerequisite-hint-empty">
                {{ prerequisiteRelationCount > 0 ? '暂无明显前置课程缺口' : '先维护前置课程关系后再检查' }}
              </div>
            </div>

            <div class="gap-layout">
              <aside v-loading="gapLoading" class="gap-report-list">
                <div class="gap-report-heading">
                  <strong>{{ gapReports.length }}</strong>
                  <span>历史报告</span>
                </div>
                <button
                  v-for="report in gapReports"
                  :key="report.id"
                  type="button"
                  :class="{ active: selectedGapReport?.id === report.id }"
                  @click="selectGapReport(report)"
                >
                  <strong>{{ report.reportName }}</strong>
                  <span>{{ report.itemCount }} 个缺口 · {{ formatGapTime(report.createTime) }}</span>
                </button>
                <div v-if="!gapLoading && gapReports.length === 0" class="gap-empty-list">
                  还没有知识缺口报告
                </div>
              </aside>

              <div class="gap-detail" v-loading="gapItemLoading">
                <div v-if="selectedGapReport" class="gap-summary">
                  <div>
                    <span>缺口总数</span>
                    <strong>{{ gapStats.total }}</strong>
                  </div>
                  <div>
                    <span>高优先级</span>
                    <strong>{{ gapStats.severe }}</strong>
                  </div>
                  <div>
                    <span>前置课程相关</span>
                    <strong>{{ gapStats.prerequisite }}</strong>
                  </div>
                </div>

                <p v-if="selectedGapReport" class="gap-report-summary">
                  {{ selectedGapReport.summary }}
                </p>

                <div v-if="selectedGapReport" class="remediation-path-panel" v-loading="remediationPathLoading">
                  <div class="remediation-path-heading">
                    <div>
                      <span>补学路径</span>
                      <strong>{{ remediationPath?.stages?.length || 0 }}</strong>
                    </div>
                    <button type="button" :disabled="remediationPathLoading" @click="loadRemediationPath">
                      {{ remediationPathLoading ? '生成中…' : '生成补学路径' }}
                    </button>
                  </div>
                  <p v-if="remediationPath" class="remediation-path-summary">{{ remediationPath.summary }}</p>
                  <div v-if="remediationPath?.stages?.length" class="remediation-stage-list">
                    <article v-for="stage in remediationPath.stages" :key="stage.stageKey">
                      <div>
                        <span>{{ stage.stageName }}</span>
                        <strong>{{ stage.items.length }} 项</strong>
                      </div>
                      <p>{{ stage.objective }}</p>
                      <ol>
                        <li v-for="item in stage.items.slice(0, 5)" :key="item.knowledgeItemId">
                          <strong>{{ item.knowledgeTitle }}</strong>
                          <span>{{ item.sourceCourseName }} · 优先级 {{ item.severityLevel }}</span>
                        </li>
                      </ol>
                    </article>
                  </div>
                </div>

                <div v-if="gapItems.length > 0" class="gap-item-grid">
                  <article v-for="item in gapItems" :key="item.id" class="gap-item-card">
                    <div class="gap-item-top">
                      <span>{{ gapTypeLabel(item.gapType) }}</span>
                      <strong>优先级 {{ item.severityLevel }}</strong>
                    </div>
                    <h3>{{ item.knowledgeTitle }}</h3>
                    <div class="gap-item-meta">
                      <span>{{ item.sourceCourseName }}</span>
                      <span>{{ relationTypeLabel(item.relationType) }}</span>
                      <span>{{ masteryStatusLabel(item.masteryStatus) }}</span>
                      <span v-if="item.examQuestionCount">真题 {{ item.examQuestionCount }} 次</span>
                    </div>
                    <p>{{ item.reason }}</p>
                    <small>{{ item.suggestion }}</small>
                  </article>
                </div>

                <div v-if="selectedGapReport && !gapItemLoading && gapItems.length === 0" class="gap-empty-detail">
                  <strong>没有发现明显知识缺口。</strong>
                  <p>可以继续补充掌握状态或真题映射后重新生成。</p>
                </div>

                <div v-if="!selectedGapReport" class="gap-empty-detail">
                  <strong>先生成一份知识缺口报告。</strong>
                  <p>报告会保存历史记录，并按优先级展示薄弱知识点。</p>
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
  gapLoading,
  gapGenerating,
  gapItemLoading,
  prerequisiteGapLoading,
  remediationPathLoading,
  gapReports,
  gapItems,
  prerequisiteGapHints,
  remediationPath,
  selectedGapReport,
  relationGroups,
  gapStats,
  prerequisiteRelationCount,
  gapForm,
  loadPrerequisiteGapHints,
  generateGapReport,
  selectGapReport,
  loadRemediationPath,
  masteryStatusLabel,
  gapTypeLabel,
  relationTypeLabel,
  formatGapTime
} = useDashboardContext()
</script>
