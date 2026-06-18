<template>
  <main v-if="!currentUser" class="auth-page">
    <section class="auth-panel">
      <div class="brand-block">
        <p class="brand-mark">AI4NOTE</p>
        <h1>课程知识，从这里开始整理</h1>
        <p>建立课程与章节结构，为资料解析、真题分析和智能复习打好基础。</p>
      </div>

      <el-form class="auth-form" label-position="top" @submit.prevent="submitAuth">
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

        <el-form-item label="用户名">
          <el-input v-model="authForm.username" maxlength="64" />
        </el-form-item>
        <el-form-item v-if="authMode === 'register'" label="邮箱">
          <el-input v-model="authForm.email" type="email" maxlength="128" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="authForm.password"
            type="password"
            show-password
            maxlength="64"
            @keyup.enter="submitAuth"
          />
        </el-form-item>
        <el-button type="primary" class="full-button" :loading="authLoading" @click="submitAuth">
          {{ authMode === 'login' ? '登录' : '创建账号' }}
        </el-button>
      </el-form>
    </section>
  </main>

  <main v-else class="workspace">
    <header class="topbar">
      <div>
        <p class="brand-mark">AI4NOTE</p>
        <h1>课程知识库</h1>
      </div>
      <div class="account-area">
        <span>{{ currentUser.username }}</span>
        <el-button text @click="logout">退出</el-button>
      </div>
    </header>

    <div class="workspace-grid">
      <aside class="course-sidebar">
        <div class="section-heading">
          <div>
            <p class="section-label">课程</p>
            <strong>{{ courses.length }} 门</strong>
          </div>
          <el-button type="primary" @click="courseDialogVisible = true">新建</el-button>
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
            <span class="course-index">{{ course.courseCode || 'COURSE' }}</span>
            <strong>{{ course.courseName }}</strong>
            <small>{{ course.semester || '未设置学期' }}</small>
          </button>

          <el-empty v-if="!courseLoading && courses.length === 0" description="暂无课程" />
        </div>
      </aside>

      <section class="course-content">
        <template v-if="selectedCourse">
          <div class="course-title-row">
            <div>
              <p class="section-label">{{ selectedCourse.courseCode || '课程知识库' }}</p>
              <h2>{{ selectedCourse.courseName }}</h2>
              <p>{{ selectedCourse.description || '暂未填写课程简介。' }}</p>
            </div>
            <el-button type="primary" plain @click="chapterDialogVisible = true">添加章节</el-button>
          </div>

          <div class="chapter-section">
            <div class="section-heading">
              <div>
                <p class="section-label">章节结构</p>
                <strong>{{ chapters.length }} 个章节</strong>
              </div>
            </div>

            <el-table v-loading="chapterLoading" :data="chapters" empty-text="暂无章节">
              <el-table-column prop="chapterNo" label="章节编号" width="160" />
              <el-table-column prop="chapterTitle" label="章节名称" />
              <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
            </el-table>
          </div>
        </template>

        <el-empty v-else description="选择或创建一门课程" />
      </section>
    </div>
  </main>

  <el-dialog v-model="courseDialogVisible" title="新建课程" width="480px">
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
      <el-button type="primary" :loading="courseSaving" @click="saveCourse">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="chapterDialogVisible" title="添加章节" width="440px">
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
      <el-button type="primary" :loading="chapterSaving" @click="saveChapter">添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login, register } from '../../api/auth'
import { createChapter, createCourse, listChapters, listCourses } from '../../api/course'

const currentUser = ref(null)
const authMode = ref('login')
const authLoading = ref(false)
const courseLoading = ref(false)
const chapterLoading = ref(false)
const courseSaving = ref(false)
const chapterSaving = ref(false)
const courseDialogVisible = ref(false)
const chapterDialogVisible = ref(false)
const courses = ref([])
const chapters = ref([])
const selectedCourse = ref(null)

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

onMounted(() => {
  const savedUser = localStorage.getItem('ai4note-user')
  if (savedUser) {
    currentUser.value = JSON.parse(savedUser)
    loadCourses()
  }
})

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
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseLoading.value = false
  }
}

async function selectCourse(course) {
  selectedCourse.value = course
  chapterLoading.value = true
  try {
    chapters.value = await listChapters(course.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    chapterLoading.value = false
  }
}

async function saveCourse() {
  if (!courseForm.courseName.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }

  courseSaving.value = true
  try {
    const course = await createCourse({
      userId: currentUser.value.id,
      courseName: courseForm.courseName.trim(),
      courseCode: courseForm.courseCode.trim() || null,
      semester: courseForm.semester.trim() || null,
      description: courseForm.description.trim() || null
    })
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
    const chapter = await createChapter(selectedCourse.value.id, currentUser.value.id, {
      chapterNo: chapterForm.chapterNo.trim(),
      chapterTitle: chapterForm.chapterTitle.trim(),
      sortOrder: chapterForm.sortOrder
    })
    chapters.value.push(chapter)
    chapters.value.sort((left, right) => left.sortOrder - right.sortOrder || left.id - right.id)
    resetChapterForm()
    chapterDialogVisible.value = false
    ElMessage.success('章节添加成功')
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
  selectedCourse.value = null
}

function resetCourseForm() {
  courseForm.courseName = ''
  courseForm.courseCode = ''
  courseForm.semester = ''
  courseForm.description = ''
}

function resetChapterForm() {
  chapterForm.chapterNo = ''
  chapterForm.chapterTitle = ''
  chapterForm.sortOrder = 0
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(34, 103, 90, 0.12), transparent 46%),
    #eef1f3;
}

.auth-panel {
  width: min(920px, 100%);
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  border: 1px solid #d8dde1;
  background: #ffffff;
  box-shadow: 0 24px 60px rgba(35, 43, 50, 0.12);
}

.brand-block,
.auth-form {
  padding: 56px;
}

.brand-block {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #173f3a;
  color: #ffffff;
}

.brand-block h1 {
  max-width: 480px;
  margin: 18px 0;
  font-size: 40px;
  line-height: 1.2;
}

.brand-block p:last-child {
  max-width: 480px;
  color: #d7e3e0;
  line-height: 1.8;
}

.brand-mark,
.section-label {
  margin: 0;
  color: #2c7568;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.brand-block .brand-mark {
  color: #9ed2c8;
}

.auth-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 28px;
  border-bottom: 1px solid #dfe3e6;
}

.auth-switch button {
  height: 44px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #66717a;
  cursor: pointer;
}

.auth-switch button.active {
  border-color: #2c7568;
  color: #173f3a;
  font-weight: 700;
}

.full-button {
  width: 100%;
}

.workspace {
  min-height: 100vh;
  background: #f3f5f6;
}

.topbar {
  height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 34px;
  border-bottom: 1px solid #dce1e4;
  background: #ffffff;
}

.topbar h1 {
  margin: 4px 0 0;
  font-size: 22px;
}

.account-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace-grid {
  min-height: calc(100vh - 83px);
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
}

.course-sidebar {
  padding: 24px 18px;
  border-right: 1px solid #dce1e4;
  background: #ffffff;
}

.section-heading,
.course-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.section-heading strong {
  display: block;
  margin-top: 4px;
}

.course-list {
  min-height: 260px;
  margin-top: 20px;
}

.course-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 15px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #253139;
  text-align: left;
  cursor: pointer;
}

.course-item:hover {
  background: #f3f7f6;
}

.course-item.active {
  border-color: #a9c8c2;
  background: #eaf3f1;
}

.course-index,
.course-item small {
  color: #738087;
  font-size: 12px;
}

.course-content {
  padding: 34px;
}

.course-title-row {
  min-height: 130px;
  padding-bottom: 30px;
  border-bottom: 1px solid #dce1e4;
}

.course-title-row h2 {
  margin: 7px 0 10px;
  font-size: 30px;
}

.course-title-row p:last-child {
  margin: 0;
  color: #66717a;
}

.chapter-section {
  margin-top: 28px;
}

.chapter-section .section-heading {
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 760px) {
  .auth-panel,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .brand-block,
  .auth-form {
    padding: 32px 24px;
  }

  .brand-block h1 {
    font-size: 30px;
  }

  .course-sidebar {
    border-right: 0;
    border-bottom: 1px solid #dce1e4;
  }

  .course-content,
  .topbar {
    padding-left: 20px;
    padding-right: 20px;
  }

  .course-title-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
