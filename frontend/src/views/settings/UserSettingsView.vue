<template>
  <main v-if="!currentUser" class="settings-empty">
    <div class="settings-empty-panel">
      <p class="eyebrow">user settings</p>
      <h1>先登录，再管理个人设置<span>.</span></h1>
      <button type="button" @click="router.push('/dashboard/overview')">返回登录</button>
    </div>
  </main>

  <main v-else class="settings-page">
    <header class="settings-topbar">
      <button type="button" class="wordmark-button" @click="router.push('/dashboard/overview')">
        ai4note<span>.</span>
      </button>
      <nav class="settings-tabs">
        <button
          v-for="section in settingSections"
          :key="section.id"
          type="button"
          :class="{ active: activePanel === section.id }"
          @click="activePanel = section.id"
        >
          {{ section.label }}
        </button>
      </nav>
      <div class="settings-account">
        <span class="account-dot"></span>
        <span>{{ currentUser.username }}</span>
        <button type="button" @click="logout">退出</button>
      </div>
    </header>

    <section class="settings-hero">
      <div>
        <p class="eyebrow">personal workspace</p>
        <h1>个人设置<span>.</span></h1>
      </div>
      <p>AI 模型供应商和生成任务记录从课程页移到这里，课程工作台只保留课程内的学习流程。</p>
    </section>

    <section v-show="activePanel === 'providers'" class="settings-section provider-section">
      <div class="settings-section-heading">
        <div>
          <p class="eyebrow">AI 供应商配置</p>
          <h2>管理模型连接<span>.</span></h2>
        </div>
        <button type="button" :disabled="aiProviderLoading" @click="loadAiProviders">
          {{ aiProviderLoading ? '刷新中...' : '刷新配置' }}
        </button>
      </div>

      <div class="provider-status">
        <div>
          <span>默认 DeepSeek</span>
          <strong>{{ aiDefaultStatus?.configured ? '已配置' : '未配置' }}</strong>
        </div>
        <div>
          <span>默认模型</span>
          <strong>{{ aiDefaultStatus?.defaultModel || '未读取' }}</strong>
        </div>
        <div>
          <span>自定义配置</span>
          <strong>{{ aiProviders.length }}</strong>
        </div>
      </div>

      <div class="provider-layout">
        <form class="provider-form" @submit.prevent="saveAiProvider">
          <label>
            <span>供应商名称</span>
            <el-input v-model="aiProviderForm.providerName" maxlength="64" />
          </label>
          <label>
            <span>Base URL</span>
            <el-input v-model="aiProviderForm.baseUrl" maxlength="255" />
          </label>
          <label>
            <span>模型名称</span>
            <el-input v-model="aiProviderForm.modelName" maxlength="128" />
          </label>
          <label>
            <span>API Key 环境变量别名</span>
            <el-input v-model="aiProviderForm.apiKeyAlias" maxlength="128" placeholder="DEEPSEEK_API_KEY" />
          </label>
          <label class="provider-switch">
            <el-switch v-model="aiProviderForm.enabled" />
            <span>启用配置</span>
          </label>
          <p>这里只保存环境变量名，不保存 API Key 明文。</p>
          <div class="provider-actions">
            <button v-if="editingAiProviderId" type="button" @click="resetAiProviderForm">取消编辑</button>
            <button type="submit" :disabled="aiProviderSaving">
              {{ aiProviderSaving ? '保存中...' : editingAiProviderId ? '保存修改' : '创建配置' }}
            </button>
          </div>
        </form>

        <div v-loading="aiProviderLoading" class="provider-list">
          <article v-for="config in aiProviders" :key="config.id" class="provider-card">
            <div class="provider-card-top">
              <span>{{ config.enabled ? '已启用' : '已停用' }}</span>
              <strong>{{ config.providerName }}</strong>
            </div>
            <h3>{{ config.modelName }}</h3>
            <p>{{ config.baseUrl }}</p>
            <small>{{ config.apiKeyAlias || '未设置 Key 别名' }}</small>
            <div class="provider-card-actions">
              <button type="button" @click="editAiProvider(config)">编辑</button>
              <button type="button" @click="removeAiProvider(config)">删除</button>
            </div>
          </article>
          <div v-if="!aiProviderLoading && aiProviders.length === 0" class="settings-empty-state">
            <strong>还没有自定义 AI 配置。</strong>
            <p>默认环境变量配置仍可直接使用。</p>
          </div>
        </div>
      </div>
    </section>

    <section v-show="activePanel === 'tasks'" class="settings-section task-section">
      <div class="settings-section-heading">
        <div>
          <p class="eyebrow">追踪生成任务</p>
          <h2>查看 AI 任务流水<span>.</span></h2>
        </div>
        <button type="button" :disabled="aiTaskLoading" @click="loadAiGenerationTasks">
          {{ aiTaskLoading ? '刷新中...' : '刷新记录' }}
        </button>
      </div>

      <div class="task-status">
        <div>
          <span>任务总数</span>
          <strong>{{ aiTaskStats.total }}</strong>
        </div>
        <div>
          <span>运行中</span>
          <strong>{{ aiTaskStats.running }}</strong>
        </div>
        <div>
          <span>失败</span>
          <strong>{{ aiTaskStats.failed }}</strong>
        </div>
      </div>

      <div v-loading="aiTaskLoading" class="task-list">
        <article v-for="task in aiGenerationTasks" :key="task.id" class="task-card">
          <div class="task-card-top">
            <span :class="`task-status-${task.status?.toLowerCase() || 'pending'}`">
              {{ aiTaskStatusLabel(task.status) }}
            </span>
            <strong>#{{ task.id }}</strong>
          </div>
          <h3>{{ aiTaskTypeLabel(task.taskType) }}</h3>
          <p>{{ task.prompt || '未保存提示词' }}</p>
          <div class="task-meta">
            <span>{{ formatAiTaskTime(task.createTime) }}</span>
            <span v-if="task.finishTime">完成：{{ formatAiTaskTime(task.finishTime) }}</span>
            <span v-if="task.resultPath">{{ task.resultPath }}</span>
          </div>
          <small v-if="task.errorMessage">{{ task.errorMessage }}</small>
          <div
            v-if="task.taskType === 'MOCK_EXAM' && task.status === 'SUCCESS' && task.resultPath"
            class="task-actions"
          >
            <button type="button" @click="downloadMockExam(task)">下载模拟题</button>
          </div>
          <div
            v-if="task.taskType === 'REVIEW_GENERATION' && task.status === 'SUCCESS' && task.resultPath?.startsWith('sprint-outlines/')"
            class="task-actions"
          >
            <button type="button" @click="downloadSprintOutline(task)">下载冲刺提纲</button>
          </div>
        </article>
        <div v-if="!aiTaskLoading && aiGenerationTasks.length === 0" class="settings-empty-state">
          <strong>还没有 AI 生成任务。</strong>
          <p>运行知识整理、教师画像或复习生成后，这里会记录状态。</p>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createAiProvider,
  deleteAiProvider,
  getAiStatus,
  listAiGenerationTasks,
  listAiProviders,
  updateAiProvider
} from '../../api/ai'
import { mockExamDownloadUrl } from '../../api/mockExam'
import { sprintOutlineDownloadUrl } from '../../api/sprintOutline'

const router = useRouter()
const currentUser = ref(null)
const activePanel = ref('providers')
const aiProviderLoading = ref(false)
const aiProviderSaving = ref(false)
const aiTaskLoading = ref(false)
const aiDefaultStatus = ref(null)
const aiProviders = ref([])
const aiGenerationTasks = ref([])
const editingAiProviderId = ref(null)

const settingSections = [
  { id: 'providers', label: 'AI 配置' },
  { id: 'tasks', label: '生成任务' }
]

const aiProviderForm = reactive({
  providerName: 'DeepSeek',
  baseUrl: 'https://api.deepseek.com',
  modelName: 'deepseek-v4-flash',
  apiKeyAlias: 'DEEPSEEK_API_KEY',
  enabled: true
})

const aiTaskStats = computed(() => ({
  total: aiGenerationTasks.value.length,
  running: aiGenerationTasks.value.filter(task => task.status === 'RUNNING' || task.status === 'PENDING').length,
  failed: aiGenerationTasks.value.filter(task => task.status === 'FAILED').length
}))

onMounted(async () => {
  const savedUser = localStorage.getItem('ai4note-user')
  if (!savedUser) {
    return
  }
  currentUser.value = JSON.parse(savedUser)
  await Promise.all([
    loadAiProviders(),
    loadAiGenerationTasks()
  ])
})

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
  if (!currentUser.value) return
  aiTaskLoading.value = true
  try {
    aiGenerationTasks.value = await listAiGenerationTasks(currentUser.value.id)
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
      aiProviders.value = aiProviders.value.map(item => item.id === saved.id ? saved : item)
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

function resetAiProviderForm() {
  editingAiProviderId.value = null
  aiProviderForm.providerName = 'DeepSeek'
  aiProviderForm.baseUrl = 'https://api.deepseek.com'
  aiProviderForm.modelName = 'deepseek-v4-flash'
  aiProviderForm.apiKeyAlias = 'DEEPSEEK_API_KEY'
  aiProviderForm.enabled = true
}

function downloadMockExam(task) {
  if (!task || !currentUser.value) return
  window.open(mockExamDownloadUrl(task.id, currentUser.value.id), '_blank')
}

function downloadSprintOutline(task) {
  if (!task || !currentUser.value) return
  window.open(sprintOutlineDownloadUrl(task.id, currentUser.value.id), '_blank')
}

function aiTaskStatusLabel(status) {
  if (status === 'PENDING') return '等待中'
  if (status === 'RUNNING') return '运行中'
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'CANCELED') return '已取消'
  return status || '未知'
}

function aiTaskTypeLabel(type) {
  if (type === 'KNOWLEDGE_GENERATION') return '知识整理'
  if (type === 'TEACHER_PROFILE') return '教师画像'
  if (type === 'MOCK_EXAM') return '模拟题生成'
  if (type === 'REVIEW_GENERATION') return '复习生成'
  if (type === 'MATERIAL_TAGGING') return '资料标签'
  return type || 'AI 任务'
}

function formatAiTaskTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function logout() {
  localStorage.removeItem('ai4note-user')
  currentUser.value = null
  router.push('/dashboard/overview')
}
</script>

<style scoped>
.settings-page,
.settings-empty {
  min-height: 100vh;
  color: #111;
  background: #f5f3ef;
}

.settings-topbar {
  min-height: 76px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0 clamp(24px, 5vw, 72px);
  border-bottom: 1px solid #111;
  background: rgba(245, 243, 239, 0.94);
  backdrop-filter: blur(18px);
}

.wordmark-button {
  border: 0;
  background: transparent;
  color: #111;
  font-size: 26px;
  font-weight: 950;
  letter-spacing: -0.04em;
  cursor: pointer;
}

.wordmark-button span,
.settings-hero h1 span,
.settings-section-heading h2 span,
.settings-empty h1 span {
  color: #ff3151;
}

.settings-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border: 1px solid #111;
  background: #fff;
}

.settings-tabs button {
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.settings-tabs button.active {
  background: #111;
  color: #fff;
}

.settings-account {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 850;
}

.settings-account button,
.settings-section-heading button,
.provider-actions button,
.provider-card-actions button,
.task-actions button,
.settings-empty-panel button {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #111;
  background: #fff;
  color: #111;
  font-weight: 900;
  cursor: pointer;
}

.settings-account button {
  min-height: 34px;
}

.account-dot {
  width: 9px;
  height: 9px;
  display: inline-block;
  background: #0de0c0;
  border-radius: 50%;
}

.settings-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.38fr);
  align-items: end;
  gap: clamp(24px, 5vw, 72px);
  padding: 76px clamp(24px, 6vw, 96px) 42px;
  border-bottom: 1px solid #111;
}

.eyebrow {
  margin: 0;
  color: #555;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.settings-hero h1,
.settings-empty h1 {
  margin: 18px 0 0;
  font-size: clamp(58px, 10vw, 136px);
  line-height: 0.85;
  letter-spacing: -0.065em;
}

.settings-hero > p {
  margin: 0;
  color: #3f4352;
  font-size: 15px;
  line-height: 1.8;
}

.settings-section {
  padding: 52px clamp(24px, 6vw, 96px) 90px;
}

.settings-section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.settings-section-heading h2 {
  margin: 14px 0 0;
  font-size: clamp(42px, 6vw, 76px);
  line-height: 0.92;
  letter-spacing: -0.06em;
}

.settings-section-heading button,
.provider-actions button:last-child,
.task-actions button {
  background: #ffb21c;
}

.provider-status,
.task-status {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 32px;
}

.provider-status > div,
.task-status > div {
  min-width: 0;
  padding: 18px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 6px 6px 0 #111;
}

.provider-status span,
.task-status span {
  display: block;
  color: #666;
  font-size: 11px;
  font-weight: 900;
}

.provider-status strong,
.task-status strong {
  display: block;
  margin-top: 8px;
  overflow: hidden;
  font-size: 30px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.38fr) minmax(0, 1fr);
  gap: 24px;
  margin-top: 32px;
}

.provider-form {
  display: grid;
  gap: 14px;
  align-content: start;
  padding: 22px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #ffb21c;
}

.provider-form label > span {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-size: 11px;
  font-weight: 900;
}

:deep(.provider-form .el-input),
:deep(.provider-form .el-select) {
  width: 100%;
}

:deep(.provider-form .el-input__wrapper) {
  min-height: 46px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.provider-switch {
  min-height: 46px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border: 1px solid #111;
}

.provider-form > p {
  margin: 0;
  color: #666;
  font-size: 11px;
  line-height: 1.6;
}

.provider-actions,
.provider-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.provider-list,
.task-list {
  min-height: 360px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
}

.provider-card,
.task-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 7px 7px 0 #111;
}

.provider-card-top,
.task-card-top,
.task-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.provider-card-top,
.task-card-top {
  justify-content: space-between;
}

.provider-card-top span,
.task-card-top span,
.task-meta span {
  padding: 4px 8px;
  border: 1px solid #111;
  font-size: 10px;
  font-weight: 900;
}

.provider-card h3,
.task-card h3 {
  margin: 16px 0 10px;
  overflow: hidden;
  font-size: 25px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-card p,
.task-card p {
  margin: 0 0 14px;
  overflow: hidden;
  color: #3f4352;
  font-size: 12px;
  line-height: 1.65;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-card small,
.task-card small {
  display: block;
  color: #ff3151;
  font-size: 11px;
  line-height: 1.6;
}

.provider-card-actions {
  margin-top: 16px;
}

.provider-card-actions button:last-child {
  color: #ff3151;
}

.task-list {
  margin-top: 32px;
}

.task-card {
  display: grid;
  gap: 12px;
}

.task-card p {
  display: -webkit-box;
  white-space: normal;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.task-status-success {
  background: #0de0c0;
}

.task-status-failed {
  background: #ff3151;
  color: #fff;
}

.task-status-running,
.task-status-pending {
  background: #ffb21c;
}

.settings-empty-state {
  grid-column: 1 / -1;
  min-height: 260px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.settings-empty-state strong {
  color: #111;
  font-size: 20px;
}

.settings-empty-state p {
  margin: 10px 0 0;
  font-size: 12px;
}

.settings-empty {
  display: grid;
  place-items: center;
  padding: 24px;
}

.settings-empty-panel {
  width: min(720px, 100%);
  padding: 42px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 12px 12px 0 #ffb21c;
}

.settings-empty-panel button {
  margin-top: 28px;
  background: #111;
  color: #fff;
}

@media (max-width: 980px) {
  .settings-topbar,
  .settings-hero,
  .settings-section-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .settings-topbar,
  .settings-account,
  .settings-section-heading {
    flex-wrap: wrap;
  }

  .settings-hero,
  .provider-layout {
    grid-template-columns: 1fr;
  }

  .provider-status,
  .task-status,
  .provider-list,
  .task-list {
    grid-template-columns: 1fr;
  }
}
</style>
