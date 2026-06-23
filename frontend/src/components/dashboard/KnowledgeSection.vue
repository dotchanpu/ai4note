<template>
<section
            id="knowledge"
            class="knowledge-section scroll-panel"
            :class="{ 'section-active': active }"
          >
            <div class="knowledge-heading">
              <div>
                <p class="eyebrow light">ai knowledge studio</p>
                <h2>不是摘词，<br>是理解后整理<span>.</span></h2>
              </div>
              <p>选择一份已经解析的资料，让 DeepSeek 按语义整理定义、重点、方法、公式、例子与易错点。</p>
            </div>

            <div class="knowledge-generator">
              <div class="knowledge-generator-main">
                <el-select
                  v-model="knowledgeForm.materialId"
                  filterable
                  placeholder="选择已解析资料"
                  @change="loadSelectedMaterialTags"
                >
                  <el-option
                    v-for="material in parsedMaterials"
                    :key="material.id"
                    :label="material.title"
                    :value="material.id"
                  />
                </el-select>
                <el-input-number v-model="knowledgeForm.maxItems" :min="1" :max="30" />
                <label class="knowledge-replace">
                  <el-switch v-model="knowledgeForm.replaceExisting" />
                  <span>替换该资料旧条目</span>
                </label>
                <button
                  type="button"
                  class="knowledge-generate-button"
                  :disabled="knowledgeGenerating || !knowledgeForm.materialId"
                  @click="generateKnowledge"
                >
                  <span>{{ knowledgeGenerating ? 'AI 整理中…' : 'AI 整理知识' }}</span>
                  <strong>✦</strong>
                </button>
              </div>
              <p>生成数量</p>
            </div>

            <div v-if="knowledgeForm.materialId" class="knowledge-tags-panel">
              <div>
                <strong>资料标签</strong>
                <span>AI 生成后仍可手动调整</span>
              </div>
              <el-select
                v-model="selectedMaterialTags"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入标签后回车"
              >
                <el-option v-for="tag in courseTags" :key="tag" :label="tag" :value="tag" />
              </el-select>
              <button type="button" :disabled="tagSaving" @click="saveSelectedMaterialTags">
                {{ tagSaving ? '保存中…' : '保存标签' }}
              </button>
              <button
                type="button"
                :disabled="tagPreviewLoading || !knowledgeForm.materialId"
                @click="previewTags"
              >
                {{ tagPreviewLoading ? '提取中…' : 'AI 提取关键词' }}
              </button>
              <div v-if="previewedMaterialTags.length" class="tag-preview-panel">
                <div>
                  <strong>候选关键词</strong>
                  <span>确认后写入资料标签</span>
                </div>
                <button
                  v-for="tag in previewedMaterialTags"
                  :key="tag"
                  type="button"
                  :class="{ active: selectedMaterialTags.includes(tag) }"
                  @click="togglePreviewTag(tag)"
                >
                  {{ tag }}
                </button>
                <button type="button" class="tag-preview-apply" @click="applyPreviewTags">
                  写入候选标签
                </button>
              </div>
            </div>

            <div class="knowledge-toolbar">
              <div>
                <strong>{{ knowledgeItems.length }}</strong>
                <span>知识条目</span>
              </div>
              <el-select
                v-model="knowledgeFilterType"
                clearable
                placeholder="全部类型"
                @change="loadKnowledgeItems"
              >
                <el-option
                  v-for="option in knowledgeTypeOptions"
                  :key="option.type"
                  :label="option.label"
                  :value="option.type"
                />
              </el-select>
            </div>

            <div v-loading="knowledgeLoading" class="knowledge-grid">
              <article
                v-for="item in knowledgeItems"
                :key="item.id"
                class="knowledge-card"
                :class="`knowledge-card-${item.itemType.toLowerCase()}`"
              >
                <div class="knowledge-card-top">
                  <span>{{ knowledgeTypeLabel(item.itemType) }}</span>
                  <div class="knowledge-stars">{{ '●'.repeat(item.importanceLevel) }}</div>
                </div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.content }}</p>
                <div class="knowledge-source">
                  <span>{{ item.materialTitle || '人工整理' }}</span>
                  <span v-if="item.sourcePage">第 {{ item.sourcePage }} 页</span>
                  <span>{{ item.chapterTitle || '未关联章节' }}</span>
                </div>
                <div class="mastery-toggle" :class="`mastery-toggle-${(item.masteryStatus || 'UNKNOWN').toLowerCase()}`">
                  <button
                    v-for="option in masteryStatusOptions"
                    :key="option.status"
                    type="button"
                    :class="{ active: (item.masteryStatus || 'UNKNOWN') === option.status }"
                    :disabled="isMasterySaving(item.id)"
                    @click="saveKnowledgeMastery(item, option.status)"
                  >
                    {{ option.label }}
                  </button>
                </div>
                <button type="button" class="danger-text" @click="confirmKnowledgeDeletion(item)">
                  删除条目
                </button>
              </article>
              <div v-if="!knowledgeLoading && knowledgeItems.length === 0" class="knowledge-empty">
                <strong>还没有知识条目。</strong>
                <p>先解析一份资料，再让 AI 从完整语义中整理知识。</p>
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
  knowledgeLoading,
  knowledgeGenerating,
  tagSaving,
  tagPreviewLoading,
  knowledgeItems,
  courseTags,
  selectedMaterialTags,
  previewedMaterialTags,
  knowledgeFilterType,
  parsedMaterials,
  knowledgeTypeOptions,
  masteryStatusOptions,
  knowledgeForm,
  loadKnowledgeItems,
  loadSelectedMaterialTags,
  saveSelectedMaterialTags,
  previewTags,
  togglePreviewTag,
  applyPreviewTags,
  generateKnowledge,
  saveKnowledgeMastery,
  isMasterySaving,
  knowledgeTypeLabel,
  confirmKnowledgeDeletion
} = useDashboardContext()
</script>
