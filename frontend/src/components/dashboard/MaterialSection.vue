<template>
<section
            id="materials"
            class="material-section scroll-panel"
            :class="{ 'section-active': active }"
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
                      <button type="button" @click="openMaterialReader(material)">预览</button>
                      <button type="button" @click="openMaterialEditor(material)">修改信息</button>
                      <button
                        v-if="canParseMaterial(material)"
                        type="button"
                        :disabled="parsingMaterialId === material.id"
                        @click="runMaterialParse(material)"
                      >
                        {{ parsingMaterialId === material.id ? '解析中…' : material.parsedChunkCount > 0 ? '重新解析' : '解析资料' }}
                      </button>
                      <button
                        v-if="material.parsedChunkCount > 0"
                        type="button"
                        :disabled="isSummaryGenerating(material.id)"
                        @click="runMaterialSummary(material)"
                      >
                        {{ isSummaryGenerating(material.id) ? '摘要中…' : material.summary ? '重新摘要' : '生成摘要' }}
                      </button>
                      <button
                        v-if="material.parsedChunkCount > 0"
                        type="button"
                        @click="showParsedText(material)"
                      >
                        查看文本 ↗
                      </button>
                      <button
                        v-if="material.materialType === 'EXAM'"
                        type="button"
                        @click="openExamWorkbench(material)"
                      >
                        exam mapping
                      </button>
                      <button type="button" class="danger-text" @click="confirmMaterialDeletion(material)">
                        删除资料
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

<script setup>
import { useDashboardContext } from './DashboardContext'

defineProps({
  active: {
    type: Boolean,
    default: false
  }
})

const {
  materialLoading,
  parsingMaterialId,
  materials,
  materialGroups,
  runMaterialParse,
  isSummaryGenerating,
  runMaterialSummary,
  canParseMaterial,
  openMaterialReader,
  showParsedText,
  openExamWorkbench,
  confirmMaterialDeletion,
  openMaterialUploader,
  openMaterialEditor,
  chapterName,
  formatFileSize
} = useDashboardContext()
</script>
