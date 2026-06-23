<template>
  <el-dialog
    v-model="courseDialogVisible"
    class="studio-dialog studio-dialog-course"
    modal-class="studio-dialog-overlay"
    width="560px"
    :show-close="false"
    @closed="resetCourseForm"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>course / 课程</p>
          <h2>{{ editingCourseId ? '修改课程信息' : '创建一门新课程' }}<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="courseDialogVisible = false">
          ×
        </button>
      </div>
    </template>
    <el-form class="studio-form" label-position="top">
      <el-form-item label="课程名称">
        <el-input v-model="courseForm.courseName" maxlength="128" placeholder="例如：编译原理" />
      </el-form-item>
      <div class="form-grid">
        <el-form-item label="课程编号">
          <el-input v-model="courseForm.courseCode" maxlength="64" placeholder="COMPILER" />
        </el-form-item>
        <el-form-item label="学期">
          <el-input v-model="courseForm.semester" maxlength="64" placeholder="2026 春" />
        </el-form-item>
      </div>
      <el-form-item label="课程简介">
        <el-input
          v-model="courseForm.description"
          type="textarea"
          :rows="4"
          placeholder="用一句话说明这门课程的学习目标。"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <button
          v-if="editingCourseId"
          type="button"
          class="dialog-button dialog-button-danger"
          :disabled="courseSaving"
          @click="confirmCourseDeletion"
        >
          删除课程
        </button>
        <button type="button" class="dialog-button dialog-button-ghost" @click="courseDialogVisible = false">
          取消
        </button>
        <button type="button" class="dialog-button dialog-button-primary" :disabled="courseSaving" @click="saveCourse">
          <span>{{ courseSaving ? '保存中…' : editingCourseId ? '保存修改' : '创建课程' }}</span>
          <strong>→</strong>
        </button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="chapterDialogVisible"
    class="studio-dialog studio-dialog-chapter"
    modal-class="studio-dialog-overlay"
    width="520px"
    :show-close="false"
    @closed="resetChapterForm"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>chapter / 章节</p>
          <h2>{{ editingChapterId ? '修改章节信息' : '添加学习章节' }}<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="chapterDialogVisible = false">
          ×
        </button>
      </div>
    </template>
    <el-form class="studio-form" label-position="top">
      <el-form-item label="章节编号">
        <el-input v-model="chapterForm.chapterNo" placeholder="例如：第1章" maxlength="64" />
      </el-form-item>
      <el-form-item label="章节名称">
        <el-input v-model="chapterForm.chapterTitle" maxlength="128" placeholder="例如：编译系统概述" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="chapterForm.sortOrder" :min="0" :max="999" class="full-number-input" />
        <p class="field-hint">数字越小，章节位置越靠前。</p>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <button
          v-if="editingChapterId"
          type="button"
          class="dialog-button dialog-button-danger"
          :disabled="chapterSaving"
          @click="confirmChapterDeletion"
        >
          删除章节
        </button>
        <button type="button" class="dialog-button dialog-button-ghost" @click="chapterDialogVisible = false">
          取消
        </button>
        <button type="button" class="dialog-button dialog-button-primary" :disabled="chapterSaving" @click="saveChapter">
          <span>{{ chapterSaving ? '保存中…' : editingChapterId ? '保存修改' : '添加章节' }}</span>
          <strong>→</strong>
        </button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="materialDialogVisible"
    class="studio-dialog studio-dialog-material"
    modal-class="studio-dialog-overlay"
    width="680px"
    :show-close="false"
    @closed="resetMaterialForm"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>material / 资料</p>
          <h2>{{ editingMaterialId ? '修改资料信息' : '放入一份新资料' }}<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="materialDialogVisible = false">
          ×
        </button>
      </div>
    </template>
    <el-form class="studio-form" label-position="top">
      <el-form-item v-if="!editingMaterialId" label="资料文件">
        <el-upload
          ref="materialUploadRef"
          class="studio-upload"
          drag
          :auto-upload="false"
          multiple
          :file-list="uploadFileList"
          accept=".pdf,.doc,.docx,.md,.txt"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <div class="upload-symbol">＋</div>
          <strong>拖入一个或多个文件，或点击选择</strong>
          <p>PDF · Word · Markdown · TXT</p>
          <template #tip>
            <div class="upload-tip">支持 PDF、Word、Markdown、TXT，单个文件不超过 50MB。</div>
          </template>
        </el-upload>
      </el-form-item>
      <div v-if="!editingMaterialId && uploadDrafts.length" class="batch-upload-list">
        <section v-for="(draft, index) in uploadDrafts" :key="draft.uid" class="batch-upload-card">
          <div class="batch-upload-card-heading">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <strong>{{ draft.name }}</strong>
              <p>{{ formatFileSize(draft.size) }}</p>
            </div>
          </div>
          <el-form-item label="资料标题">
            <el-input v-model="draft.title" maxlength="255" placeholder="输入一个清楚、容易检索的标题" />
          </el-form-item>
          <div class="form-grid">
            <el-form-item label="资料类型">
              <el-select v-model="draft.materialType" class="full-select">
                <el-option label="课件" value="SLIDE" />
                <el-option label="实验报告" value="LAB_REPORT" />
                <el-option label="往年真题" value="EXAM" />
                <el-option label="复习笔记" value="NOTE" />
                <el-option label="代码样例" value="CODE" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="所属章节">
              <el-select v-model="draft.chapterId" clearable class="full-select">
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
              <el-input-number v-model="draft.year" :min="1900" :max="2100" />
            </el-form-item>
            <el-form-item label="重点资料">
              <el-switch v-model="draft.isKey" />
            </el-form-item>
          </div>
          <el-form-item label="摘要">
            <el-input
              v-model="draft.summary"
              type="textarea"
              :rows="2"
              placeholder="简单描述资料内容、用途或重点。"
            />
          </el-form-item>
        </section>
      </div>
      <template v-if="editingMaterialId">
        <el-form-item label="资料标题">
          <el-input v-model="materialForm.title" maxlength="255" placeholder="输入一个清楚、容易检索的标题" />
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
          <el-input
            v-model="materialForm.summary"
            type="textarea"
            :rows="3"
            placeholder="简单描述资料内容、用途或重点。"
          />
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <button
          v-if="editingMaterialId"
          type="button"
          class="dialog-button dialog-button-danger"
          :disabled="materialSaving"
          @click="confirmMaterialDeletion"
        >
          删除资料
        </button>
        <button type="button" class="dialog-button dialog-button-ghost" @click="materialDialogVisible = false">
          取消
        </button>
        <button type="button" class="dialog-button dialog-button-primary" :disabled="materialSaving" @click="saveMaterial">
          <span>{{ materialSaving ? '保存中…' : editingMaterialId ? '保存修改' : '上传资料' }}</span>
          <strong>→</strong>
        </button>
      </div>
    </template>
  </el-dialog>

  <MaterialReaderDialog
    v-model="textPreviewVisible"
    :material="previewMaterial"
    :mode="materialPreviewMode"
    :loading="textChunkLoading"
    :chunks="textChunks"
    :content="materialPreviewContent"
    :reader-url="materialReaderUrl"
    :footer-text="materialReaderFooter"
    :can-download="canDownloadPreviewMaterial"
    :render-markdown="renderMarkdown"
    @download="downloadPreviewMaterial"
  />

  <el-dialog
    v-model="similarDialogVisible"
    class="studio-dialog studio-dialog-similar"
    modal-class="studio-dialog-overlay"
    width="680px"
    :show-close="false"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>similar / 相似资料</p>
          <h2>可能已有重复资料<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="similarDialogVisible = false">
          ×
        </button>
      </div>
    </template>
    <div class="similar-dialog">
      <p v-if="similarSourceMaterial">
        「{{ similarSourceMaterial.title }}」与以下资料内容接近，建议确认后再继续整理。
      </p>
      <article v-for="item in similarMaterialResults" :key="item.material.id" class="similar-material-card">
        <div>
          <strong>{{ item.material.title }}</strong>
          <span>{{ materialTypeLabel(item.material.materialType) }} · {{ Math.round(item.score * 100) }}%</span>
        </div>
        <p>{{ item.material.summary || '暂无摘要' }}</p>
        <div class="similar-reasons">
          <span v-for="reason in item.reasons" :key="reason">{{ reason }}</span>
        </div>
        <button type="button" @click="openSimilarMaterial(item.material)">查看资料</button>
      </article>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <button type="button" class="dialog-button dialog-button-primary" @click="similarDialogVisible = false">
          <span>我知道了</span>
          <strong>→</strong>
        </button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
    v-model="deleteDialogVisible"
    class="studio-dialog studio-dialog-delete"
    modal-class="studio-dialog-overlay"
    width="520px"
    :show-close="false"
    @closed="resetDeleteTarget"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>delete / 删除</p>
          <h2>{{ deleteDialogTitle }}<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="deleteDialogVisible = false">
          ×
        </button>
      </div>
    </template>
    <div class="delete-dialog-copy">
      <strong>{{ deleteTarget?.name }}</strong>
      <p>{{ deleteDialogDescription }}</p>
      <div class="delete-warning">此操作无法撤销，请确认后再继续。</div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <button type="button" class="dialog-button dialog-button-ghost" @click="deleteDialogVisible = false">
          取消
        </button>
        <button
          type="button"
          class="dialog-button dialog-button-danger-confirm"
          :disabled="deleteLoading"
          @click="executeDeletion"
        >
          <span>{{ deleteLoading ? '删除中…' : '确认删除' }}</span>
          <strong>×</strong>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'
import MaterialReaderDialog from './MaterialReaderDialog.vue'

const {
  courseDialogVisible,
  editingCourseId,
  resetCourseForm
} = useDashboardContext()
</script>
