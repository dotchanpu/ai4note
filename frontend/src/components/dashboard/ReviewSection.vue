<template>
  <section
    id="review"
    class="review-section scroll-panel"
    :class="{ 'section-active': active }"
  >
    <div class="review-heading">
      <div>
        <p class="eyebrow">复习配置</p>
        <h2>定义生成目标<span>.</span></h2>
      </div>
      <p>保存考试目标、难度、输出类型、前置课程、教师画像和自定义要求，后续可直接用于复习资料生成。</p>
    </div>

    <div class="review-layout">
      <form class="review-form review-form-horizontal" @submit.prevent="$emit('save-profile')">
        <label>
          <span>配置名称</span>
          <el-input v-model="profileForm.profileName" maxlength="128" />
        </label>
        <label>
          <span>考试目标</span>
          <el-input v-model="profileForm.target" maxlength="128" placeholder="例如：期末考试" />
        </label>
        <label>
          <span>复习难度</span>
          <el-select v-model="profileForm.difficultyLevel">
            <el-option
              v-for="option in difficultyOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>
        <label>
          <span>教师画像</span>
          <el-select v-model="profileForm.teacherProfileId" clearable placeholder="不绑定画像">
            <el-option
              v-for="profile in teacherProfiles"
              :key="profile.id"
              :label="profile.teacherName"
              :value="profile.id"
            />
          </el-select>
        </label>
        <label class="review-switch">
          <el-switch v-model="profileForm.includePrerequisites" />
          <span>包含前置课程</span>
        </label>
        <label class="review-wide">
          <span>自定义要求</span>
          <el-input
            v-model="profileForm.customRequirement"
            type="textarea"
            :rows="2"
            placeholder="例如：重点覆盖树、图和排序，输出适合考前两天冲刺的版本"
          />
        </label>
        <div class="review-form-actions">
          <button v-if="editingProfileId" type="button" @click="$emit('reset-profile')">
            取消编辑
          </button>
          <button type="submit" :disabled="profileSaving">
            {{ profileSaving ? '保存中…' : editingProfileId ? '保存修改' : '保存配置' }}
          </button>
        </div>
      </form>

      <div class="review-generation-panel">
        <div class="review-generation-heading">
          <div>
            <span>生成工具</span>
            <strong>选择要生成的复习资料</strong>
          </div>
          <p>{{ outputLabel(activeOutputType) }} 会按照当前横向配置和课程知识点生成 Markdown，并在下方直接预览。</p>
        </div>

        <div class="review-output-grid">
          <button
            v-for="option in outputOptions"
            :key="option.value"
            type="button"
            class="review-output-card"
            :class="{ active: activeOutputType === option.value }"
            :style="{ '--output-accent': option.accent }"
            @click="$emit('select-output', option.value)"
          >
            <span>{{ option.label }}</span>
            <strong>{{ option.value === 'MOCK_EXAM' ? mockExamForm.questionCount + ' 题' : 'MD' }}</strong>
            <small>{{ option.hint }}</small>
          </button>
        </div>

        <div class="review-generator-surface">
          <form class="review-generator-controls" @submit.prevent="$emit('generate')">
            <label v-if="activeOutputType === 'MOCK_EXAM'">
              <span>题数</span>
              <el-input-number
                v-model="mockExamForm.questionCount"
                :min="1"
                :max="30"
                :step="1"
                controls-position="right"
              />
            </label>
            <label v-if="activeOutputType === 'MOCK_EXAM'">
              <span>难度</span>
              <el-select v-model="mockExamForm.difficultyLevel">
                <el-option
                  v-for="option in difficultyOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>
            <label class="review-generator-requirement">
              <span>本次补充要求</span>
              <el-input
                :model-value="customRequirement"
                maxlength="1000"
                placeholder="可留空，默认使用上方自定义要求"
                @update:model-value="$emit('update:customRequirement', $event)"
              />
            </label>
            <button type="submit" :disabled="generating">
              {{ generating ? '生成中…' : '生成' + outputLabel(activeOutputType) }}
            </button>
          </form>

          <div v-if="activeResult" class="review-result-panel">
            <div class="review-result-heading">
              <div>
                <span>{{ outputLabel(activeResult.outputType) }}</span>
                <strong>{{ activeResult.title }}</strong>
                <small>{{ activeResult.resultPath }}</small>
              </div>
              <button type="button" @click="$emit('download-result', activeResult)">
                下载 Markdown
              </button>
            </div>
            <div class="markdown-preview" v-html="renderMarkdown(activeResult.content)"></div>
          </div>
          <div v-else class="review-result-empty">
            <strong>生成后会在这里展示 Markdown 预览。</strong>
            <p>标题、列表、表格、勾选项都会按 Markdown 结构渲染。</p>
          </div>
        </div>
      </div>

      <div v-loading="profileLoading" class="review-profile-list review-profile-row">
        <article v-for="profile in profiles" :key="profile.id" class="review-profile-card">
          <div class="review-card-top">
            <span>{{ outputLabel(profile.outputType) }}</span>
            <strong>{{ difficultyLabel(profile.difficultyLevel) }}</strong>
          </div>
          <h3>{{ profile.profileName }}</h3>
          <p>{{ profile.customRequirement || '暂无自定义要求' }}</p>
          <div class="review-card-meta">
            <span>{{ profile.target || '未设置目标' }}</span>
            <span>{{ profile.includePrerequisites ? '包含前置课程' : '仅当前课程' }}</span>
            <span>{{ profile.teacherName || '未绑定教师画像' }}</span>
          </div>
          <div class="review-card-actions">
            <button type="button" @click="$emit('edit-profile', profile)">编辑</button>
            <button type="button" @click="$emit('remove-profile', profile)">删除</button>
          </div>
        </article>
        <div
          v-if="!profileLoading && profiles.length === 0"
          class="review-empty"
        >
          <strong>还没有复习配置。</strong>
          <p>先保存一个横向配置，后续生成复习资料时可以复用。</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  active: {
    type: Boolean,
    default: false
  },
  profileForm: {
    type: Object,
    required: true
  },
  mockExamForm: {
    type: Object,
    required: true
  },
  teacherProfiles: {
    type: Array,
    default: () => []
  },
  difficultyOptions: {
    type: Array,
    default: () => []
  },
  outputOptions: {
    type: Array,
    default: () => []
  },
  activeOutputType: {
    type: String,
    default: 'REVIEW_NOTE'
  },
  customRequirement: {
    type: String,
    default: ''
  },
  generating: {
    type: Boolean,
    default: false
  },
  activeResult: {
    type: Object,
    default: null
  },
  profiles: {
    type: Array,
    default: () => []
  },
  profileLoading: {
    type: Boolean,
    default: false
  },
  profileSaving: {
    type: Boolean,
    default: false
  },
  editingProfileId: {
    type: [Number, String],
    default: null
  },
  outputLabel: {
    type: Function,
    required: true
  },
  difficultyLabel: {
    type: Function,
    required: true
  },
  renderMarkdown: {
    type: Function,
    required: true
  }
})

defineEmits([
  'save-profile',
  'reset-profile',
  'select-output',
  'generate',
  'update:customRequirement',
  'download-result',
  'edit-profile',
  'remove-profile'
])
</script>

<style scoped>
.review-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  color: #111;
  background: #f5f3ef;
}

.review-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.review-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.review-heading h2 span {
  color: #ffb21c;
}

.review-heading > p {
  margin: 0 0 5px;
  color: #3f4352;
  font-size: 15px;
  line-height: 1.75;
}

.review-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  margin-top: 48px;
}

.review-generation-panel {
  display: grid;
  gap: 20px;
  padding: 22px;
  border: 1px solid #111;
  background: #fff8e8;
  color: #111;
  box-shadow: 10px 10px 0 #ffb21c;
}

.review-generation-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.review-generation-heading > p {
  max-width: 520px;
  margin: 0;
  color: #4d4d4d;
  font-size: 13px;
  line-height: 1.7;
}

.review-generation-heading span,
.review-generation-heading strong {
  display: block;
}

.review-generation-heading span {
  color: #ffb21c;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.review-generation-heading strong {
  font-size: 24px;
  letter-spacing: -0.04em;
}

.review-output-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.review-output-card {
  min-height: 138px;
  display: grid;
  align-content: space-between;
  gap: 10px;
  padding: 16px;
  border: 1px solid #111;
  background: #fff;
  color: #111;
  text-align: left;
  box-shadow: 5px 5px 0 var(--output-accent);
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}

.review-output-card:hover,
.review-output-card.active {
  background: var(--output-accent);
  transform: translateY(-3px);
}

.review-output-card span,
.review-output-card strong,
.review-output-card small {
  display: block;
}

.review-output-card span {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.review-output-card strong {
  justify-self: start;
  padding: 4px 8px;
  border: 1px solid #111;
  background: #fff;
  font-size: 11px;
  font-weight: 900;
}

.review-output-card small {
  color: #333;
  font-size: 12px;
  line-height: 1.55;
}

.review-generator-surface {
  display: grid;
  grid-template-columns: minmax(280px, 0.32fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.review-generator-controls {
  display: grid;
  gap: 14px;
  align-content: start;
  padding: 18px;
  border: 1px solid #111;
  background: #fff;
}

.review-generator-controls label > span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 900;
}

:deep(.review-generator-controls .el-input),
:deep(.review-generator-controls .el-input-number),
:deep(.review-generator-controls .el-select) {
  width: 100%;
}

:deep(.review-generator-controls .el-input__wrapper),
:deep(.review-generator-controls .el-input-number .el-input__wrapper),
:deep(.review-generator-controls .el-select__wrapper) {
  min-height: 46px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.review-generator-controls > button {
  min-height: 46px;
  border: 1px solid #111;
  background: #ffb21c;
  color: #111;
  font-weight: 900;
  cursor: pointer;
}

.review-generator-controls > button:disabled {
  opacity: 0.65;
  cursor: wait;
}

.review-result-panel,
.review-result-empty {
  min-width: 0;
  border: 1px solid #111;
  background: #fff;
}

.review-result-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid #111;
  background: #fff;
}

.review-result-heading span,
.review-result-heading strong,
.review-result-heading small {
  display: block;
}

.review-result-heading span {
  color: #ff3151;
  font-size: 11px;
  font-weight: 900;
}

.review-result-heading strong {
  margin-top: 6px;
  font-size: 22px;
  letter-spacing: -0.04em;
}

.review-result-heading small {
  margin-top: 6px;
  color: #777;
  font-size: 11px;
}

.review-result-heading button {
  min-height: 40px;
  flex: 0 0 auto;
  padding: 0 14px;
  border: 1px solid #111;
  background: #ffb21c;
  font-weight: 900;
  cursor: pointer;
}

.review-result-empty {
  min-height: 320px;
  display: grid;
  place-content: center;
  justify-items: center;
  padding: 24px;
  border-style: dashed;
  color: #666;
  text-align: center;
}

.review-result-empty strong {
  color: #111;
  font-size: 20px;
}

.review-result-empty p {
  margin: 10px 0 0;
  font-size: 12px;
}

.markdown-preview {
  max-height: 620px;
  overflow: auto;
  padding: 22px;
  color: #191919;
  line-height: 1.78;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3),
.markdown-preview :deep(h4) {
  margin: 22px 0 12px;
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.markdown-preview :deep(h1) {
  margin-top: 0;
  font-size: 34px;
}

.markdown-preview :deep(h2) {
  font-size: 26px;
  border-bottom: 1px solid #111;
  padding-bottom: 8px;
}

.markdown-preview :deep(h3) {
  font-size: 21px;
}

.markdown-preview :deep(p) {
  margin: 0 0 12px;
}

.markdown-preview :deep(ul) {
  margin: 0 0 16px;
  padding-left: 20px;
}

.markdown-preview :deep(li) {
  margin: 6px 0;
}

.markdown-preview :deep(.markdown-check) {
  list-style: none;
  position: relative;
  margin-left: -18px;
  padding-left: 28px;
}

.markdown-preview :deep(.markdown-check::before) {
  content: "";
  position: absolute;
  left: 0;
  top: 0.45em;
  width: 14px;
  height: 14px;
  border: 1px solid #111;
  background: #fff;
}

.markdown-preview :deep(.markdown-check.checked::before) {
  background: #0de0c0;
  box-shadow: inset 0 0 0 3px #fff;
}

.markdown-preview :deep(table) {
  width: 100%;
  margin: 14px 0 18px;
  border-collapse: collapse;
  font-size: 13px;
}

.markdown-preview :deep(th),
.markdown-preview :deep(td) {
  padding: 10px;
  border: 1px solid #111;
  text-align: left;
  vertical-align: top;
}

.markdown-preview :deep(th) {
  background: #fff0c7;
}

.markdown-preview :deep(code) {
  padding: 2px 5px;
  border: 1px solid #ddd;
  background: #f5f3ef;
  font-family: Consolas, monospace;
}

.review-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr)) auto;
  gap: 14px;
  align-items: end;
  padding: 22px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #ffb21c;
}

.review-form-horizontal .review-wide {
  grid-column: span 2;
}

.review-form-horizontal .review-form-actions {
  align-self: end;
}

.review-form label > span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

:deep(.review-form .el-select),
:deep(.review-form .el-input),
:deep(.review-form .el-input-number) {
  width: 100%;
}

:deep(.review-form .el-select__wrapper),
:deep(.review-form .el-input__wrapper),
:deep(.review-form .el-textarea__inner) {
  min-height: 48px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.review-switch {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border: 1px solid #111;
  font-weight: 850;
}

.review-wide {
  min-width: 0;
}

.review-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.review-form-actions button {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid #111;
  background: #fff;
  font-weight: 850;
  cursor: pointer;
}

.review-form-actions button:last-child {
  background: #ffb21c;
}

.review-form-actions button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.review-profile-list {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 14px;
  align-content: start;
}

.review-profile-row {
  margin-top: 4px;
}

.review-profile-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 7px 7px 0 #111;
}

.review-card-top,
.review-card-meta,
.review-card-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.review-card-top {
  justify-content: space-between;
}

.review-card-top span,
.review-card-meta span {
  padding: 4px 8px;
  border: 1px solid #111;
  font-size: 10px;
  font-weight: 850;
}

.review-card-top strong {
  color: #ff3151;
  font-size: 12px;
}

.review-profile-card h3 {
  margin: 16px 0 10px;
  font-size: 25px;
  letter-spacing: -0.04em;
}

.review-profile-card p {
  min-height: 52px;
  margin: 0 0 14px;
  color: #333;
  line-height: 1.65;
}

.review-card-actions {
  justify-content: flex-end;
  margin-top: 16px;
}

.review-card-actions button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #111;
  background: #fff;
  font-size: 11px;
  font-weight: 850;
  cursor: pointer;
}

.review-card-actions button:last-child {
  color: #ff3151;
}

.review-empty {
  grid-column: 1 / -1;
  min-height: 320px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.review-empty strong {
  color: #111;
  font-size: 20px;
}

.review-empty p {
  margin: 10px 0 0;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .review-section {
    padding-left: 22px;
    padding-right: 22px;
  }

  .review-generator-surface,
  .review-form,
  .review-profile-list {
    grid-template-columns: 1fr;
  }

  .review-output-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-generation-heading,
  .review-heading {
    grid-template-columns: 1fr;
  }

  .review-heading,
  .review-generation-heading,
  .review-result-heading {
    display: grid;
  }

  .review-heading > p {
    margin-top: 24px;
  }

  .review-form-actions,
  .review-form-horizontal .review-wide {
    grid-column: auto;
  }

  .review-form-actions {
    display: grid;
    justify-items: stretch;
  }
}
</style>
