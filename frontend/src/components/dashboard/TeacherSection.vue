<template>
<section
            id="teacher"
            class="teacher-section scroll-panel"
            :class="{ 'section-active': active }"
          >
            <div class="teacher-heading">
              <div>
                <p class="eyebrow">教师画像</p>
                <h2>分析出题风格<span>.</span></h2>
              </div>
              <p>基于课程资料和往年真题归纳教师的出题风格、题型偏好、评分偏好和复习重点。</p>
            </div>

            <div class="teacher-layout">
              <aside class="teacher-control">
                <div class="teacher-form">
                  <label>
                    <span>教师名称</span>
                    <el-input v-model="teacherProfileForm.teacherName" maxlength="128" placeholder="例如：张老师" />
                  </label>
                  <label>
                    <span>分析资料</span>
                    <el-select
                      v-model="teacherProfileForm.materialIds"
                      multiple
                      collapse-tags
                      clearable
                      placeholder="默认使用全部可分析资料"
                    >
                      <el-option
                        v-for="material in teacherProfileMaterials"
                        :key="material.id"
                        :label="material.title"
                        :value="material.id"
                      />
                    </el-select>
                  </label>
                  <button
                    type="button"
                    :disabled="teacherProfileAnalyzing"
                    @click="runTeacherProfileAnalysis"
                  >
                    {{ teacherProfileAnalyzing ? '分析中…' : '开始分析' }}
                  </button>
                </div>

                <div v-loading="teacherProfileLoading" class="teacher-profile-list">
                  <button
                    v-for="profile in teacherProfiles"
                    :key="profile.id"
                    type="button"
                    :class="{ active: selectedTeacherProfile?.id === profile.id }"
                    @click="selectTeacherProfile(profile)"
                  >
                    <strong>{{ profile.teacherName }}</strong>
                    <span>{{ teacherStatusLabel(profile.analysisStatus) }}</span>
                  </button>
                  <div
                    v-if="!teacherProfileLoading && teacherProfiles.length === 0"
                    class="teacher-empty-list"
                  >
                    还没有教师画像
                  </div>
                </div>
              </aside>

              <div class="teacher-profile-detail">
                <template v-if="selectedTeacherProfile">
                  <div class="teacher-profile-top">
                    <div>
                      <span>{{ teacherStatusLabel(selectedTeacherProfile.analysisStatus) }}</span>
                      <h3>{{ selectedTeacherProfile.teacherName }}</h3>
                    </div>
                    <div class="teacher-profile-score">
                      <strong>{{ selectedTeacherProfile.confidenceScore ?? 0 }}%</strong>
                      <button type="button" :disabled="teacherConfidenceScoring" @click="recalculateTeacherConfidence">
                        {{ teacherConfidenceScoring ? '评分中…' : '重算置信度' }}
                      </button>
                      <button type="button" :disabled="teacherProfileReanalyzing" @click="runTeacherProfileReanalysis">
                        {{ teacherProfileReanalyzing ? '分析中…' : '重新分析' }}
                      </button>
                      <button type="button" @click="openTeacherProfileEditor">编辑画像</button>
                    </div>
                  </div>
                  <div v-if="teacherProfileEditing" class="teacher-edit-panel">
                    <div class="teacher-edit-grid">
                      <label>
                        <span>教师名称</span>
                        <el-input v-model="teacherEditForm.teacherName" maxlength="128" />
                      </label>
                      <label>
                        <span>置信度</span>
                        <el-input-number
                          v-model="teacherEditForm.confidenceScore"
                          :min="0"
                          :max="100"
                          :step="5"
                          controls-position="right"
                        />
                      </label>
                      <label>
                        <span>出题风格</span>
                        <el-input v-model="teacherEditForm.examStyle" type="textarea" :rows="3" />
                      </label>
                      <label>
                        <span>题型偏好</span>
                        <el-input v-model="teacherEditForm.questionPreference" type="textarea" :rows="3" />
                      </label>
                      <label>
                        <span>评分偏好</span>
                        <el-input v-model="teacherEditForm.gradingPreference" type="textarea" :rows="3" />
                      </label>
                      <label>
                        <span>重点章节</span>
                        <el-input v-model="teacherEditForm.focusTopics" type="textarea" :rows="3" />
                      </label>
                      <label>
                        <span>规避内容</span>
                        <el-input v-model="teacherEditForm.avoidTopics" type="textarea" :rows="3" />
                      </label>
                      <label>
                        <span>依据摘要</span>
                        <el-input v-model="teacherEditForm.sourceSummary" type="textarea" :rows="3" />
                      </label>
                    </div>
                    <div class="teacher-edit-actions">
                      <button type="button" @click="cancelTeacherProfileEdit">取消</button>
                      <button type="button" :disabled="teacherProfileSaving" @click="saveTeacherProfileEdit">
                        {{ teacherProfileSaving ? '保存中…' : '保存并确认' }}
                      </button>
                    </div>
                  </div>
                  <div class="teacher-profile-grid">
                    <section>
                      <span>出题风格</span>
                      <p>{{ selectedTeacherProfile.examStyle || '暂无分析结果' }}</p>
                    </section>
                    <section>
                      <span>题型偏好</span>
                      <p>{{ selectedTeacherProfile.questionPreference || '暂无分析结果' }}</p>
                    </section>
                    <section>
                      <span>评分偏好</span>
                      <p>{{ selectedTeacherProfile.gradingPreference || '暂无分析结果' }}</p>
                    </section>
                    <section>
                      <span>重点章节</span>
                      <p>{{ selectedTeacherProfile.focusTopics || '暂无分析结果' }}</p>
                    </section>
                    <section>
                      <span>规避内容</span>
                      <p>{{ selectedTeacherProfile.avoidTopics || '暂无分析结果' }}</p>
                    </section>
                    <section>
                      <span>依据摘要</span>
                      <p>{{ selectedTeacherProfile.sourceSummary || '暂无分析结果' }}</p>
                    </section>
                  </div>
                  <div v-loading="teacherEvidenceLoading" class="teacher-evidence-panel">
                    <div class="teacher-evidence-heading">
                      <strong>{{ teacherProfileEvidence.length }}</strong>
                      <span>证据来源</span>
                    </div>
                    <div v-if="teacherProfileEvidence.length > 0" class="teacher-evidence-list">
                      <article v-for="evidence in teacherProfileEvidence" :key="evidence.id">
                        <div>
                          <strong>{{ evidence.materialTitle }}</strong>
                          <span>{{ evidenceTypeLabel(evidence.evidenceType) }}</span>
                        </div>
                        <p>{{ evidence.evidenceSummary }}</p>
                        <small>
                          {{ evidence.materialType || '资料' }}
                          <template v-if="evidence.sourcePage"> · 第 {{ evidence.sourcePage }} 页</template>
                          <template v-if="evidence.confidenceScore !== null"> · 置信度 {{ evidence.confidenceScore }}%</template>
                        </small>
                      </article>
                    </div>
                    <div v-else class="teacher-evidence-empty">
                      暂无证据来源
                    </div>
                  </div>
                </template>
                <div v-else class="teacher-empty-detail">
                  <strong>先生成教师画像。</strong>
                  <p>建议选择课件、真题和评分相关资料一起分析。</p>
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
  teacherProfileLoading,
  teacherProfileAnalyzing,
  teacherEvidenceLoading,
  teacherProfileEditing,
  teacherProfileSaving,
  teacherConfidenceScoring,
  teacherProfileReanalyzing,
  teacherProfiles,
  teacherProfileEvidence,
  selectedTeacherProfile,
  teacherProfileMaterials,
  teacherProfileForm,
  teacherEditForm,
  runTeacherProfileAnalysis,
  selectTeacherProfile,
  recalculateTeacherConfidence,
  runTeacherProfileReanalysis,
  openTeacherProfileEditor,
  cancelTeacherProfileEdit,
  saveTeacherProfileEdit,
  teacherStatusLabel,
  evidenceTypeLabel
} = useDashboardContext()
</script>
