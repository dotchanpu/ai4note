<template>
  <main v-if="!currentUser" class="auth-page">
    <div class="auth-shape auth-shape-yellow"></div>
    <div class="auth-shape auth-shape-mint"></div>
    <div class="auth-shape auth-shape-cyan"></div>
    <div class="auth-shape auth-shape-pink"></div>

    <header class="auth-header">
      <div class="wordmark">ai4note<span>.</span></div>
      <p>course knowledge studio</p>
    </header>

    <section class="auth-stage">
      <div class="auth-intro">
        <p class="eyebrow">整理你的课程世界</p>
        <h1>
          learn.<br>
          organize.<br>
          <span>remember.</span>
        </h1>
        <p class="auth-copy">
          把课件、真题、笔记和实验资料放进同一个知识空间，
          让每一次复习都更准确、更有方向。
        </p>
      </div>

      <el-form class="auth-form bold-form" label-position="top" @submit.prevent="submitAuth">
        <p class="form-kicker">{{ authMode === 'login' ? 'welcome back.' : 'start here.' }}</p>
        <div class="auth-switch">
          <button
            type="button"
            :class="{ active: authMode === 'login' }"
            @click="authMode = 'login'"
          >
            登录
          </button>
          <button
            type="button"
            :class="{ active: authMode === 'register' }"
            @click="authMode = 'register'"
          >
            注册
          </button>
        </div>

        <el-form-item label="用户名 / username">
          <el-input v-model="authForm.username" maxlength="64" placeholder="输入用户名" />
        </el-form-item>
        <el-form-item v-if="authMode === 'register'" label="邮箱 / email">
          <el-input v-model="authForm.email" type="email" maxlength="128" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="密码 / password">
          <el-input
            v-model="authForm.password"
            type="password"
            show-password
            maxlength="64"
            placeholder="至少 6 位"
            @keyup.enter="submitAuth"
          />
        </el-form-item>
        <button type="button" class="bold-submit" :disabled="authLoading" @click="submitAuth">
          <span>{{ authMode === 'login' ? '进入知识库' : '创建我的知识库' }}</span>
          <strong>→</strong>
        </button>
      </el-form>
    </section>
  </main>

  <main v-else class="workspace">
    <header class="topbar">
      <div class="wordmark workspace-wordmark">ai4note<span>.</span></div>
      <nav class="topnav">
        <button
          v-for="section in pageSections"
          :key="section.id"
          type="button"
          :class="{ active: activeSection === section.id }"
          @click="scrollToSection(section.id)"
        >
          {{ section.label }}
        </button>
      </nav>
      <div class="account-area">
        <span class="account-dot"></span>
        <span>{{ currentUser.username }}</span>
        <button type="button" class="plain-action" @click="logout">退出</button>
      </div>
    </header>

    <div class="workspace-layout">
      <aside class="course-sidebar">
        <div class="sidebar-heading">
          <p>your courses.</p>
          <span>{{ String(courses.length).padStart(2, '0') }}</span>
        </div>

        <div v-loading="courseLoading" class="course-list">
          <button
            v-for="course in courses"
            :key="course.id"
            type="button"
            class="course-item"
            :class="{ active: selectedCourse?.id === course.id }"
            @click="selectCourse(course)"
          >
            <span class="course-number">{{ String(courses.indexOf(course) + 1).padStart(2, '0') }}</span>
            <span class="course-item-copy">
              <strong>{{ course.courseName }}</strong>
              <small>{{ course.courseCode || 'COURSE' }} · {{ course.semester || '未设置学期' }}</small>
            </span>
            <span class="course-arrow">↗</span>
          </button>

          <div v-if="!courseLoading && courses.length === 0" class="sidebar-empty">
            还没有课程，从第一门开始。
          </div>
        </div>

        <button type="button" class="new-course-button" @click="openCourseCreator">
          <span>新建课程</span>
          <strong>＋</strong>
        </button>
      </aside>

      <section class="course-content">
        <template v-if="selectedCourse">
          <nav class="section-dots" aria-label="页面章节">
            <button
              v-for="(section, index) in pageSections"
              :key="section.id"
              type="button"
              :class="{ active: activeSection === section.id }"
              :aria-label="section.title"
              @click="scrollToSection(section.id)"
            >
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
            </button>
          </nav>

          <section
            id="overview"
            class="course-hero scroll-panel"
            :class="{ 'section-active': activeSection === 'overview' }"
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
              </div>
            </div>
            <div class="hero-stats">
              <div>
                <strong>{{ String(chapters.length).padStart(2, '0') }}</strong>
                <span>chapters</span>
              </div>
              <div>
                <strong>{{ String(materials.length).padStart(2, '0') }}</strong>
                <span>materials</span>
              </div>
              <div>
                <strong>{{ materials.filter(item => item.parsedChunkCount > 0).length }}</strong>
                <span>parsed</span>
              </div>
            </div>
          </section>

          <section
            id="chapters"
            class="chapter-section scroll-panel"
            :class="{ 'section-active': activeSection === 'chapters' }"
          >
            <div class="section-intro">
              <div>
                <p class="eyebrow light">course map</p>
                <h2>章节不是目录，<br>是你的学习路径<span>.</span></h2>
              </div>
              <button type="button" class="light-pill" @click="openChapterCreator">添加章节 ＋</button>
            </div>

            <div v-loading="chapterLoading" class="chapter-track">
              <article v-for="(chapter, index) in chapters" :key="chapter.id" class="chapter-card">
                <span class="chapter-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <div>
                  <p>{{ chapter.chapterNo }}</p>
                  <h3>{{ chapter.chapterTitle }}</h3>
                </div>
                <button type="button" @click="openChapterEditor(chapter)">edit ↗</button>
              </article>
              <div v-if="!chapterLoading && chapters.length === 0" class="dark-empty">
                添加章节，搭建这门课程的学习路线。
              </div>
            </div>
          </section>

          <section
            id="materials"
            class="material-section scroll-panel"
            :class="{ 'section-active': activeSection === 'materials' }"
          >
            <div class="material-title-row">
              <div>
                <p class="eyebrow">knowledge materials</p>
                <h2>所有资料，<br>各就各位<span>.</span></h2>
              </div>
              <p>
                按资料类型分区整理。每张卡片都保留来源、章节、
                解析状态与重点标记。
              </p>
            </div>

            <div v-loading="materialLoading" class="material-groups">
              <section
                v-for="group in materialGroups"
                :key="group.type"
                class="material-group"
                :class="`material-group-${group.type.toLowerCase()}`"
              >
                <div class="material-group-heading">
                  <div>
                    <span class="material-group-icon">{{ group.icon }}</span>
                    <div>
                      <strong>{{ group.label }}<span>.</span></strong>
                      <small>{{ group.type.toLowerCase() }}</small>
                    </div>
                  </div>
                  <div class="group-heading-actions">
                    <span class="group-count">{{ String(group.items.length).padStart(2, '0') }}</span>
                    <button
                      type="button"
                      class="group-add-button"
                      :aria-label="`添加${group.label}`"
                      @click="openMaterialUploader(group.type)"
                    >
                      ＋
                    </button>
                  </div>
                </div>

                <div v-if="group.items.length" class="material-card-grid">
                  <article v-for="material in group.items" :key="material.id" class="material-card">
                    <div class="material-card-top">
                      <div class="material-file-badge">{{ (material.fileType || 'FILE').toUpperCase() }}</div>
                      <div class="material-status">
                        <span v-if="material.key" class="status-dot status-key">重点</span>
                        <span v-if="material.parsedChunkCount > 0" class="status-dot status-parsed">
                          已解析
                        </span>
                      </div>
                    </div>
                    <h3>{{ material.title }}</h3>
                    <p class="material-summary">{{ material.summary || '暂无资料摘要' }}</p>
                    <div class="material-meta">
                      <span>{{ chapterName(material.chapterId) }}</span>
                      <span>{{ material.year || '未标年份' }}</span>
                      <span>{{ formatFileSize(material.fileSize) }}</span>
                    </div>
                    <p class="material-file-name">{{ material.originalName }}</p>
                    <div class="material-card-actions">
                      <button type="button" @click="openMaterialEditor(material)">修改信息</button>
                      <button
                        v-if="material.fileType === 'pdf'"
                        type="button"
                        :disabled="parsingMaterialId === material.id"
                        @click="runPdfParse(material)"
                      >
                        {{ parsingMaterialId === material.id ? '解析中…' : '解析 PDF' }}
                      </button>
                      <button
                        v-if="material.parsedChunkCount > 0"
                        type="button"
                        @click="showParsedText(material)"
                      >
                        查看文本 ↗
                      </button>
                    </div>
                  </article>
                </div>
                <button
                  v-else
                  type="button"
                  class="material-group-empty"
                  @click="openMaterialUploader(group.type)"
                >
                  <span>＋</span>
                  <p>暂无{{ group.label }}</p>
                  <strong>添加{{ group.label }}</strong>
                </button>
              </section>
            </div>
          </section>
        </template>

        <section v-else class="no-course">
          <div class="no-course-shape"></div>
          <p class="eyebrow">your first course</p>
          <h1>从一门课程开始<span>.</span></h1>
          <button type="button" class="primary-pill" @click="openCourseCreator">新建课程 ＋</button>
        </section>
      </section>
    </div>
  </main>

  <el-dialog
    v-model="courseDialogVisible"
    :title="editingCourseId ? '修改课程信息' : '新建课程'"
    width="480px"
    @closed="resetCourseForm"
  >
    <el-form label-position="top">
      <el-form-item label="课程名称">
        <el-input v-model="courseForm.courseName" maxlength="128" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="课程编号">
          <el-input v-model="courseForm.courseCode" maxlength="64" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="courseForm.semester" maxlength="64" />
        </el-form-item>
      </div>
      <el-form-item label="课程简介">
        <el-input v-model="courseForm.description" type="textarea" :rows="4" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="courseDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="courseSaving" @click="saveCourse">
        {{ editingCourseId ? '保存修改' : '创建' }}
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="chapterDialogVisible"
    :title="editingChapterId ? '修改章节信息' : '添加章节'"
    width="440px"
    @closed="resetChapterForm"
  >
    <el-form label-position="top">
      <el-form-item label="章节编号">
        <el-input v-model="chapterForm.chapterNo" placeholder="例如：第1章" maxlength="64" />
      </el-form-item>
      <el-form-item label="章节名称">
        <el-input v-model="chapterForm.chapterTitle" maxlength="128" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="chapterForm.sortOrder" :min="0" :max="999" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="chapterDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="chapterSaving" @click="saveChapter">
        {{ editingChapterId ? '保存修改' : '添加' }}
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="materialDialogVisible"
    :title="editingMaterialId ? '修改资料信息' : '上传课程资料'"
    width="540px"
    @closed="resetMaterialForm"
  >
    <el-form label-position="top">
      <el-form-item v-if="!editingMaterialId" label="资料文件">
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept=".pdf,.doc,.docx,.md,.txt"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <el-button>选择文件</el-button>
          <template #tip>
            <div class="upload-tip">支持 PDF、Word、Markdown、TXT，单个文件不超过 50MB。</div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item label="资料标题">
        <el-input v-model="materialForm.title" maxlength="255" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="资料类型">
          <el-select v-model="materialForm.materialType" class="full-select">
            <el-option label="课件" value="SLIDE" />
            <el-option label="实验报告" value="LAB_REPORT" />
            <el-option label="往年真题" value="EXAM" />
            <el-option label="复习笔记" value="NOTE" />
            <el-option label="代码样例" value="CODE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属章节">
          <el-select v-model="materialForm.chapterId" clearable class="full-select">
            <el-option
              v-for="chapter in chapters"
              :key="chapter.id"
              :label="`${chapter.chapterNo} ${chapter.chapterTitle}`"
              :value="chapter.id"
            />
          </el-select>
        </el-form-item>
      </div>
      <div class="form-grid">
        <el-form-item label="年份">
          <el-input-number v-model="materialForm.year" :min="1900" :max="2100" />
        </el-form-item>
        <el-form-item label="重点资料">
          <el-switch v-model="materialForm.isKey" />
        </el-form-item>
      </div>
      <el-form-item label="摘要">
        <el-input v-model="materialForm.summary" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="materialDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="materialSaving" @click="saveMaterial">
        {{ editingMaterialId ? '保存修改' : '上传' }}
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="textPreviewVisible"
    :title="`${previewMaterial?.title || ''} - 解析文本`"
    width="760px"
  >
    <div v-loading="textChunkLoading" class="text-preview">
      <section v-for="chunk in textChunks" :key="chunk.id" class="text-page">
        <div class="text-page-heading">
          <strong>第 {{ chunk.pageNo }} 页</strong>
          <span>{{ chunk.wordCount }} 字符</span>
        </div>
        <pre>{{ chunk.content }}</pre>
      </section>
      <el-empty v-if="!textChunkLoading && textChunks.length === 0" description="暂无解析文本" />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login, register } from '../../api/auth'
import {
  createChapter,
  createCourse,
  listChapters,
  listCourses,
  updateChapter,
  updateCourse
} from '../../api/course'
import {
  listMaterials,
  listTextChunks,
  parsePdf,
  updateMaterial,
  uploadMaterial
} from '../../api/material'

const currentUser = ref(null)
const authMode = ref('login')
const authLoading = ref(false)
const courseLoading = ref(false)
const chapterLoading = ref(false)
const materialLoading = ref(false)
const courseSaving = ref(false)
const chapterSaving = ref(false)
const materialSaving = ref(false)
const parsingMaterialId = ref(null)
const textChunkLoading = ref(false)
const courseDialogVisible = ref(false)
const chapterDialogVisible = ref(false)
const materialDialogVisible = ref(false)
const textPreviewVisible = ref(false)
const editingCourseId = ref(null)
const editingChapterId = ref(null)
const editingMaterialId = ref(null)
const courses = ref([])
const chapters = ref([])
const materials = ref([])
const selectedCourse = ref(null)
const selectedFile = ref(null)
const previewMaterial = ref(null)
const textChunks = ref([])
const activeSection = ref('overview')

const pageSections = [
  { id: 'overview', label: 'overview.', title: '课程概览' },
  { id: 'chapters', label: 'chapters.', title: '章节路径' },
  { id: 'materials', label: 'materials.', title: '课程资料' }
]

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

onMounted(() => {
  const savedUser = localStorage.getItem('ai4note-user')
  if (savedUser) {
    currentUser.value = JSON.parse(savedUser)
    loadCourses()
  }
})

function scrollToSection(sectionId) {
  if (pageSections.some(section => section.id === sectionId)) {
    activeSection.value = sectionId
  }
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
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseLoading.value = false
  }
}

async function selectCourse(course) {
  selectedCourse.value = course
  activeSection.value = 'overview'
  chapterLoading.value = true
  materialLoading.value = true
  try {
    const [chapterData, materialData] = await Promise.all([
      listChapters(course.id, currentUser.value.id),
      listMaterials(course.id, currentUser.value.id)
    ])
    chapters.value = chapterData
    materials.value = materialData
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    chapterLoading.value = false
    materialLoading.value = false
  }
}

function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw
  if (!materialForm.title && uploadFile.name) {
    materialForm.title = uploadFile.name.replace(/\.[^.]+$/, '')
  }
}

function handleFileRemove() {
  selectedFile.value = null
}

async function saveMaterial() {
  if (!editingMaterialId.value && !selectedFile.value) {
    ElMessage.warning('请选择资料文件')
    return
  }
  if (!materialForm.title.trim()) {
    ElMessage.warning('请输入资料标题')
    return
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
      materialDialogVisible.value = false
      ElMessage.success('资料信息已更新')
      return
    }

    const data = new FormData()
    data.append('userId', currentUser.value.id)
    data.append('courseId', selectedCourse.value.id)
    data.append('title', materialForm.title.trim())
    data.append('materialType', materialForm.materialType)
    data.append('year', materialForm.year)
    data.append('isKey', materialForm.isKey)
    data.append('summary', materialForm.summary.trim())
    data.append('file', selectedFile.value)
    if (materialForm.chapterId) {
      data.append('chapterId', materialForm.chapterId)
    }

    const material = await uploadMaterial(data)
    materials.value.unshift(material)
    resetMaterialForm()
    materialDialogVisible.value = false
    ElMessage.success('资料上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    materialSaving.value = false
  }
}

async function runPdfParse(material) {
  parsingMaterialId.value = material.id
  try {
    const result = await parsePdf(material.id, currentUser.value.id)
    material.parsedChunkCount = result.chunkCount
    ElMessage.success(`解析完成，共提取 ${result.chunkCount} 个文本块`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    parsingMaterialId.value = null
  }
}

async function showParsedText(material) {
  previewMaterial.value = material
  textPreviewVisible.value = true
  textChunkLoading.value = true
  try {
    textChunks.value = await listTextChunks(material.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    textChunkLoading.value = false
  }
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

function logout() {
  localStorage.removeItem('ai4note-user')
  currentUser.value = null
  courses.value = []
  chapters.value = []
  materials.value = []
  selectedCourse.value = null
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
  editingMaterialId.value = null
  materialForm.title = ''
  materialForm.materialType = 'SLIDE'
  materialForm.chapterId = null
  materialForm.year = new Date().getFullYear()
  materialForm.isKey = false
  materialForm.summary = ''
  selectedFile.value = null
}

function formatFileSize(size) {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(html) {
  scroll-behavior: smooth;
}

:global(body) {
  margin: 0;
  color: #0d0d0d;
  background: #fff;
  font-family: Inter, "Helvetica Neue", Arial, "PingFang SC", "Microsoft YaHei", sans-serif;
}

button,
a {
  font: inherit;
}

button {
  color: inherit;
}

.wordmark {
  position: relative;
  z-index: 2;
  color: #0a0a0a;
  font-size: clamp(32px, 4vw, 58px);
  font-weight: 300;
  letter-spacing: -0.08em;
  line-height: 0.9;
}

.wordmark span,
.course-hero h1 span,
.section-intro h2 span,
.material-title-row h2 span,
.material-group-heading strong span,
.no-course h1 span {
  color: #ff3151;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.eyebrow.light {
  color: #72f1d3;
}

.auth-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #fff;
}

.auth-header {
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 4vw;
  border-bottom: 1px solid #111;
}

.auth-header .wordmark {
  font-size: 42px;
}

.auth-header p {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.auth-stage {
  position: relative;
  z-index: 1;
  min-height: calc(100vh - 96px);
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(360px, 0.75fr);
  align-items: center;
  gap: 7vw;
  padding: 5vh 7vw 7vh;
}

.auth-intro h1 {
  margin: 20px 0 26px;
  font-size: clamp(70px, 9.5vw, 150px);
  font-weight: 800;
  letter-spacing: -0.085em;
  line-height: 0.78;
}

.auth-intro h1 span {
  position: relative;
  color: #0d0d0d;
}

.auth-intro h1 span::after {
  content: "";
  position: absolute;
  right: -0.08em;
  bottom: 0.08em;
  width: 0.22em;
  height: 0.22em;
  border-radius: 50%;
  background: #ff3151;
}

.auth-copy {
  max-width: 560px;
  margin: 0;
  font-size: clamp(16px, 1.5vw, 21px);
  line-height: 1.7;
}

.auth-form {
  position: relative;
  padding: clamp(28px, 4vw, 54px);
  border: 2px solid #111;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 18px 18px 0 #0de0c0;
}

.form-kicker {
  margin: 0 0 26px;
  font-size: clamp(30px, 4vw, 54px);
  font-weight: 800;
  letter-spacing: -0.05em;
}

.auth-switch {
  display: flex;
  gap: 8px;
  margin-bottom: 26px;
}

.auth-switch button {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #111;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}

.auth-switch button.active {
  background: #111;
  color: #fff;
}

.bold-submit {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 17px 22px;
  border: 2px solid #111;
  border-radius: 999px;
  background: #ffb21c;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.bold-submit:hover {
  transform: translate(-3px, -3px);
  box-shadow: 6px 6px 0 #111;
}

.bold-submit strong {
  font-size: 24px;
}

.auth-shape {
  position: absolute;
  pointer-events: none;
}

.auth-shape-yellow {
  top: 18%;
  left: 43%;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #ffb21c;
}

.auth-shape-mint {
  top: 8%;
  right: 7%;
  width: 180px;
  height: 180px;
  border-radius: 50% 50% 10% 50%;
  background: #0de0c0;
}

.auth-shape-cyan {
  right: 3%;
  bottom: 4%;
  width: 38px;
  height: 230px;
  background: #14cbea;
}

.auth-shape-pink {
  bottom: 7%;
  left: 4%;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #ffc0d0;
}

.workspace {
  min-height: 100vh;
  background: #fff;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 86px;
  display: grid;
  grid-template-columns: 240px 1fr auto;
  align-items: center;
  gap: 24px;
  padding: 0 32px;
  border-bottom: 1px solid #111;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
}

.workspace-wordmark {
  font-size: 36px;
}

.topnav {
  display: flex;
  justify-content: center;
  gap: clamp(22px, 4vw, 58px);
}

.topnav button {
  padding: 8px 0;
  border: 0;
  background: transparent;
  color: #111;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  opacity: 0.42;
  transition: opacity 0.25s, transform 0.25s;
}

.topnav button:hover,
.topnav button.active {
  opacity: 1;
  transform: translateY(-2px);
}

.topnav button.active {
  text-decoration: underline;
  text-decoration-color: #ff3151;
  text-decoration-thickness: 3px;
  text-underline-offset: 7px;
}

.account-area {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
}

.account-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #0de0c0;
}

.plain-action {
  padding: 7px 12px;
  border: 1px solid #111;
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
}

.workspace-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
}

.course-sidebar {
  position: sticky;
  top: 86px;
  height: calc(100vh - 86px);
  display: flex;
  flex-direction: column;
  padding: 28px 22px 22px;
  border-right: 1px solid #111;
  background: #fff;
}

.sidebar-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 22px;
  border-bottom: 1px solid #111;
}

.sidebar-heading p {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.sidebar-heading span {
  font-size: 12px;
  font-weight: 800;
}

.course-list {
  min-height: 180px;
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.course-item {
  width: 100%;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 10px;
  padding: 18px 4px;
  border: 0;
  border-bottom: 1px solid #dadada;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.course-item:hover,
.course-item.active {
  background: #f6f6f6;
}

.course-item.active {
  box-shadow: inset 5px 0 0 #ff3151;
}

.course-number {
  color: #888;
  font-size: 11px;
  font-weight: 700;
}

.course-item-copy {
  min-width: 0;
}

.course-item-copy strong,
.course-item-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-item-copy strong {
  font-size: 15px;
}

.course-item-copy small {
  margin-top: 5px;
  color: #777;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.course-arrow {
  font-size: 18px;
}

.sidebar-empty {
  padding: 30px 8px;
  color: #777;
  font-size: 13px;
  line-height: 1.6;
}

.new-course-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 18px;
  border: 1px solid #111;
  border-radius: 999px;
  background: #111;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.new-course-button strong {
  font-size: 22px;
}

.course-content {
  position: relative;
  height: calc(100vh - 86px);
  min-width: 0;
  overflow: hidden;
}

.scroll-panel {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transform: translateY(56px) scale(0.985);
  transition:
    opacity 0.6s ease,
    transform 0.9s cubic-bezier(0.16, 1, 0.3, 1),
    visibility 0s linear 0.9s;
  scrollbar-width: thin;
}

.scroll-panel.section-active {
  z-index: 2;
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transform: translateY(0) scale(1);
  transition:
    opacity 0.6s ease,
    transform 0.9s cubic-bezier(0.16, 1, 0.3, 1),
    visibility 0s;
}

.section-dots {
  position: fixed;
  z-index: 15;
  top: 50%;
  right: 18px;
  display: grid;
  gap: 10px;
  transform: translateY(-50%);
}

.section-dots button {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid #111;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.86);
  font-size: 9px;
  font-weight: 900;
  cursor: pointer;
  transition: transform 0.25s, background 0.25s, color 0.25s;
}

.section-dots button.active {
  background: #111;
  color: #fff;
  transform: scale(1.2);
}

.course-hero {
  min-height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 48px;
  overflow: hidden;
  padding: 70px clamp(38px, 6vw, 90px) 58px;
  border-bottom: 1px solid #111;
}

.hero-copy {
  position: relative;
  z-index: 2;
  max-width: 900px;
  opacity: 0;
  transform: translateY(55px);
  transition: opacity 0.75s ease, transform 0.75s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.course-hero h1 {
  max-width: 900px;
  margin: 24px 0 26px;
  font-size: clamp(62px, 8vw, 128px);
  font-weight: 800;
  letter-spacing: -0.075em;
  line-height: 0.84;
  word-break: break-word;
}

.hero-copy > p:last-of-type {
  max-width: 620px;
  margin: 0;
  font-size: clamp(16px, 1.6vw, 21px);
  line-height: 1.65;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 34px;
}

.primary-pill,
.outline-pill,
.light-pill {
  padding: 15px 22px;
  border: 1px solid #111;
  border-radius: 999px;
  font-weight: 800;
  cursor: pointer;
}

.primary-pill {
  background: #ffb21c;
}

.primary-pill span {
  margin-left: 22px;
}

.outline-pill {
  background: #fff;
}

.primary-pill:hover,
.outline-pill:hover {
  transform: translateY(-2px);
}

.hero-stats {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 12px;
  opacity: 0;
  transform: translateX(48px);
  transition: opacity 0.7s 0.18s ease, transform 0.7s 0.18s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.hero-stats > div {
  min-width: 150px;
  padding: 20px;
  border: 1px solid #111;
  background: rgba(255, 255, 255, 0.9);
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  font-size: 38px;
  line-height: 1;
}

.hero-stats span {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-orbit {
  position: absolute;
}

.hero-orbit-one {
  top: 58px;
  right: 20%;
  width: 210px;
  height: 210px;
  border-radius: 50% 50% 8% 50%;
  background: #0de0c0;
  opacity: 0;
  transform: translateY(-70px) rotate(-18deg) scale(0.65);
  transition: opacity 0.8s 0.1s ease, transform 1s 0.1s cubic-bezier(0.16, 1, 0.3, 1);
}

.hero-orbit-two {
  top: 72px;
  right: 8%;
  width: 46px;
  height: 250px;
  background: #14cbea;
  opacity: 0;
  transform: translateY(-100px);
  transition: opacity 0.7s 0.24s ease, transform 0.9s 0.24s cubic-bezier(0.16, 1, 0.3, 1);
}

.course-hero.section-active .hero-copy,
.course-hero.section-active .hero-stats,
.course-hero.section-active .hero-orbit-one,
.course-hero.section-active .hero-orbit-two {
  opacity: 1;
  transform: translate(0) rotate(0) scale(1);
}

.chapter-section {
  min-height: 100%;
  padding: 74px clamp(38px, 6vw, 90px) 86px;
  color: #fff;
  background: #111;
}

.section-intro {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 28px;
  opacity: 0;
  transform: translateY(55px);
  transition: opacity 0.7s ease, transform 0.75s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.section-intro h2,
.material-title-row h2 {
  margin: 18px 0 0;
  font-size: clamp(46px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.light-pill {
  border-color: #fff;
  background: transparent;
  color: #fff;
}

.light-pill:hover {
  background: #fff;
  color: #111;
}

.chapter-track {
  min-height: 140px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  margin-top: 54px;
  background: #111;
}

.chapter-card {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px;
  border: 1px solid #3b3b3b;
  background: #171717;
  opacity: 0;
  transform: translateY(45px);
  transition:
    background 0.2s,
    opacity 0.6s ease,
    transform 0.7s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.chapter-card:nth-child(2) {
  transition-delay: 0.08s;
}

.chapter-card:nth-child(3) {
  transition-delay: 0.16s;
}

.chapter-card:nth-child(4) {
  transition-delay: 0.24s;
}

.chapter-card:nth-child(5) {
  transition-delay: 0.32s;
}

.chapter-card:nth-child(6) {
  transition-delay: 0.4s;
}

.chapter-section.section-active .section-intro,
.chapter-section.section-active .chapter-card,
.chapter-section.section-active .dark-empty {
  opacity: 1;
  transform: translateY(0);
}

.chapter-card:hover {
  background: #242424;
}

.chapter-index {
  color: #72f1d3;
  font-size: 12px;
  font-weight: 800;
}

.chapter-card p {
  margin: 0 0 8px;
  color: #999;
  font-size: 12px;
}

.chapter-card h3 {
  margin: 0;
  font-size: clamp(21px, 2.2vw, 32px);
  line-height: 1.1;
}

.chapter-card button {
  align-self: flex-end;
  border: 0;
  background: transparent;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.dark-empty {
  grid-column: 1 / -1;
  padding: 58px;
  background: #171717;
  color: #aaa;
  text-align: center;
  opacity: 0;
  transform: translateY(35px);
  transition: opacity 0.6s 0.15s ease, transform 0.6s 0.15s ease;
}

.material-section {
  min-height: 100%;
  padding: 80px clamp(38px, 6vw, 90px) 100px;
  background: #f5f3ef;
}

.material-title-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.45fr);
  align-items: end;
  gap: 50px;
  opacity: 0;
  transform: translateY(55px);
  transition: opacity 0.7s ease, transform 0.75s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.material-title-row > p {
  margin: 0 0 6px;
  font-size: 16px;
  line-height: 1.7;
}

.material-groups {
  min-height: 220px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
  margin-top: 60px;
}

.material-group {
  --accent: #ffb21c;
  min-width: 0;
  padding: 24px;
  border: 1px solid #111;
  background: #fff;
  opacity: 0;
  transform: translateY(48px) scale(0.97);
  transition: opacity 0.6s ease, transform 0.7s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.material-group:nth-child(2) {
  transition-delay: 0.08s;
}

.material-group:nth-child(3) {
  transition-delay: 0.16s;
}

.material-group:nth-child(4) {
  transition-delay: 0.24s;
}

.material-group:nth-child(5) {
  transition-delay: 0.32s;
}

.material-group:nth-child(6) {
  transition-delay: 0.4s;
}

.material-section.section-active .material-title-row,
.material-section.section-active .material-group {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.material-group-lab_report {
  --accent: #0de0c0;
}

.material-group-exam {
  --accent: #ff3151;
}

.material-group-note {
  --accent: #ffc0d0;
}

.material-group-code {
  --accent: #14cbea;
}

.material-group-other {
  --accent: #ad93ff;
}

.material-group-heading,
.material-group-heading > div,
.material-card-top,
.material-status,
.material-meta,
.material-card-actions {
  display: flex;
  align-items: center;
}

.material-group-heading {
  justify-content: space-between;
  padding-bottom: 20px;
  border-bottom: 1px solid #111;
}

.material-group-heading > div {
  gap: 12px;
}

.group-heading-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.material-group-heading strong,
.material-group-heading small {
  display: block;
}

.material-group-heading strong {
  font-size: 22px;
  letter-spacing: -0.03em;
}

.material-group-heading small {
  margin-top: 3px;
  color: #777;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.material-group-icon {
  min-width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--accent);
  font-size: 10px;
  font-weight: 900;
}

.group-count {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.06em;
}

.group-add-button {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid #111;
  border-radius: 50%;
  background: var(--accent);
  font-size: 21px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s;
}

.group-add-button:hover {
  transform: rotate(90deg);
}

.material-card-grid {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.material-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 7px 7px 0 var(--accent);
  transition: transform 0.2s, box-shadow 0.2s;
}

.material-card:hover {
  transform: translate(-3px, -3px);
  box-shadow: 10px 10px 0 var(--accent);
}

.material-card-top {
  justify-content: space-between;
  gap: 10px;
}

.material-file-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: #111;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

.material-status {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}

.status-dot {
  padding: 5px 8px;
  border: 1px solid #111;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
}

.status-key {
  background: #ffc0d0;
}

.status-parsed {
  background: #72f1d3;
}

.material-card h3 {
  margin: 22px 0 10px;
  overflow: hidden;
  font-size: clamp(21px, 2.2vw, 30px);
  letter-spacing: -0.04em;
  line-height: 1.05;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-summary {
  min-height: 44px;
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: #565656;
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.material-meta {
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 18px;
  font-size: 11px;
  font-weight: 700;
}

.material-meta span::before {
  content: "•";
  margin-right: 6px;
  color: var(--accent);
}

.material-file-name {
  margin: 14px 0 0;
  overflow: hidden;
  color: #888;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-card-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-top: 16px;
  padding-top: 13px;
  border-top: 1px solid #111;
}

.material-card-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
}

.material-card-actions button:hover {
  text-decoration: underline;
  text-underline-offset: 4px;
}

.material-card-actions button:disabled {
  opacity: 0.45;
  cursor: wait;
}

.material-group-empty {
  width: 100%;
  min-height: 160px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 20px;
  border: 0;
  background: transparent;
  color: #777;
  font-family: inherit;
  text-align: center;
  cursor: pointer;
}

.material-group-empty span {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border: 1px solid #111;
  border-radius: 50%;
  color: #111;
  font-size: 22px;
}

.material-group-empty p {
  margin: 0;
  font-size: 12px;
}

.material-group-empty strong {
  color: #111;
  font-size: 11px;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s, transform 0.2s;
}

.material-group-empty:hover span {
  background: var(--accent);
}

.material-group-empty:hover strong {
  opacity: 1;
  transform: translateY(0);
}

.no-course {
  position: relative;
  min-height: calc(100vh - 86px);
  display: grid;
  place-content: center;
  justify-items: start;
  overflow: hidden;
  padding: 60px;
}

.no-course h1 {
  max-width: 760px;
  margin: 20px 0 32px;
  font-size: clamp(70px, 10vw, 150px);
  letter-spacing: -0.08em;
  line-height: 0.82;
}

.no-course-shape {
  position: absolute;
  top: 16%;
  right: 12%;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: #0de0c0;
}

.full-select {
  width: 100%;
}

.upload-tip {
  margin-top: 7px;
  color: #777;
  font-size: 12px;
}

.text-preview {
  min-height: 180px;
  max-height: 65vh;
  overflow: auto;
}

.text-page {
  padding: 16px 0 22px;
  border-bottom: 1px solid #ddd;
}

.text-page:first-child {
  padding-top: 0;
}

.text-page-heading {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #666;
  font-size: 13px;
}

.text-page pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Arial, "Microsoft YaHei", sans-serif;
  font-size: 14px;
  line-height: 1.75;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

:deep(.bold-form .el-form-item__label) {
  color: #111;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

:deep(.bold-form .el-input__wrapper) {
  padding: 6px 14px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

:deep(.bold-form .el-input__wrapper.is-focus) {
  box-shadow: inset 0 -3px 0 #ff3151;
}

:deep(.el-dialog) {
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: 14px 14px 0 #0de0c0;
}

:deep(.el-dialog__title) {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #111;
  --el-button-border-color: #111;
  --el-button-hover-bg-color: #ff3151;
  --el-button-hover-border-color: #ff3151;
}

@media (max-width: 1080px) {
  .topbar {
    grid-template-columns: auto 1fr;
  }

  .topnav {
    display: none;
  }

  .account-area {
    justify-self: end;
  }

  .workspace-layout {
    grid-template-columns: 230px minmax(0, 1fr);
  }

  .course-hero {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: repeat(3, 1fr);
  }

  .hero-stats > div {
    min-width: 0;
  }

  .chapter-track {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .auth-header {
    height: 76px;
    padding: 18px 20px;
  }

  .auth-header .wordmark {
    font-size: 32px;
  }

  .auth-header p {
    display: none;
  }

  .auth-stage {
    min-height: calc(100vh - 76px);
    grid-template-columns: 1fr;
    gap: 42px;
    padding: 48px 20px 70px;
  }

  .auth-intro h1 {
    font-size: clamp(58px, 19vw, 92px);
  }

  .auth-shape-yellow {
    top: 12%;
    left: auto;
    right: 8%;
    width: 74px;
    height: 74px;
  }

  .auth-shape-mint,
  .auth-shape-cyan {
    display: none;
  }

  .auth-form {
    box-shadow: 9px 9px 0 #0de0c0;
  }

  .topbar {
    height: 72px;
    padding: 0 18px;
  }

  .workspace-wordmark {
    font-size: 30px;
  }

  .account-area > span:not(.account-dot) {
    display: none;
  }

  .workspace-layout {
    display: block;
  }

  .course-content {
    height: calc(100vh - 72px);
    overflow: hidden;
  }

  .scroll-panel {
    min-height: 100%;
  }

  .section-dots {
    position: fixed;
    top: auto;
    right: 50%;
    bottom: 14px;
    display: flex;
    padding: 6px;
    border: 1px solid #111;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.92);
    transform: translateX(50%);
  }

  .course-sidebar {
    position: static;
    height: auto;
    padding: 18px;
    border-right: 0;
    border-bottom: 1px solid #111;
  }

  .course-list {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    padding: 12px 0;
  }

  .course-item {
    min-width: 240px;
    border: 1px solid #111;
    padding: 14px;
  }

  .course-item.active {
    box-shadow: inset 0 -5px 0 #ff3151;
  }

  .course-hero,
  .chapter-section,
  .material-section {
    padding-left: 22px;
    padding-right: 22px;
  }

  .course-hero {
    min-height: 560px;
    padding-top: 58px;
  }

  .course-hero h1 {
    font-size: clamp(54px, 17vw, 86px);
  }

  .hero-orbit-one {
    top: 32px;
    right: -50px;
    width: 150px;
    height: 150px;
  }

  .hero-orbit-two {
    display: none;
  }

  .hero-stats {
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
  }

  .hero-stats > div {
    padding: 13px;
  }

  .hero-stats strong {
    font-size: 28px;
  }

  .section-intro,
  .material-title-row {
    display: block;
  }

  .section-intro h2,
  .material-title-row h2 {
    font-size: clamp(42px, 13vw, 66px);
  }

  .light-pill {
    margin-top: 26px;
  }

  .chapter-track,
  .material-groups {
    grid-template-columns: 1fr;
  }

  .material-title-row > p {
    margin-top: 24px;
  }

  .material-groups {
    margin-top: 40px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
