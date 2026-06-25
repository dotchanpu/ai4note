<template>
<section
            id="search"
            class="search-section scroll-panel"
            :class="{ 'section-active': active }"
          >
            <div class="search-heading">
              <div>
                <p class="eyebrow">course search</p>
                <h2>详细搜索<br>课程内容<span>.</span></h2>
              </div>
              <p>统一检索资料标题、摘要、标签、正文，以及 AI 整理出的知识条目。</p>
            </div>

            <div class="search-panel">
              <div class="search-input-row">
                <el-input
                  v-model="searchForm.keyword"
                  clearable
                  maxlength="255"
                  placeholder="输入知识点、标签、题目关键词或资料名称"
                  @keyup.enter="runSearch()"
                />
                <button type="button" :disabled="searchLoading" @click="runSearch()">
                  <span>{{ searchLoading ? '检索中…' : '开始检索' }}</span>
                  <strong>→</strong>
                </button>
              </div>
              <div class="search-filters">
                <el-select v-model="searchForm.chapterId" clearable placeholder="全部章节">
                  <el-option
                    v-for="chapter in chapters"
                    :key="chapter.id"
                    :label="`${chapter.chapterNo} ${chapter.chapterTitle}`"
                    :value="chapter.id"
                  />
                </el-select>
                <el-select v-model="searchForm.materialType" clearable placeholder="全部资料类型">
                  <el-option
                    v-for="option in materialTypeOptions"
                    :key="option.type"
                    :label="option.label"
                    :value="option.type"
                  />
                </el-select>
                <label class="search-key-filter">
                  <el-switch v-model="searchForm.isKey" />
                  <span>只看重点资料</span>
                </label>
                <button type="button" class="search-reset" @click="resetSearch">清空筛选</button>
              </div>
            </div>

            <div class="search-history" v-loading="searchRecordLoading">
              <div class="search-history-heading">
                <div>
                  <strong>{{ searchRecords.length }}</strong>
                  <span>历史搜索</span>
                </div>
                <button type="button" :disabled="searchRecordLoading" @click="loadSearchRecords">
                  刷新历史
                </button>
              </div>
              <div v-if="searchRecords.length > 0" class="search-history-list">
                <button
                  v-for="record in searchRecords"
                  :key="record.id"
                  type="button"
                  @click="rerunSearchRecord(record)"
                >
                  <strong>{{ record.keyword }}</strong>
                  <span>{{ searchTypeLabel(record.searchType) }}</span>
                  <span>{{ record.resultCount }} 条结果</span>
                  <small>{{ formatSearchTime(record.searchTime) }}</small>
                </button>
              </div>
              <div v-else class="search-history-empty">
                <strong>还没有搜索记录。</strong>
                <p>完成一次课程检索后，关键词和结果数量会出现在这里。</p>
              </div>
            </div>

            <div class="search-result-heading">
              <strong>{{ searchHasRun ? `${searchResults.length} 条结果` : '等待检索' }}</strong>
              <span v-if="searchHasRun && searchForm.keyword">“{{ searchForm.keyword }}”</span>
            </div>

            <div v-loading="searchLoading" class="search-results">
              <article
                v-for="(result, index) in searchResults"
                :key="`${result.resultType}-${result.knowledgeItemId || result.materialId}`"
                class="search-result-card"
                :class="{ 'search-result-knowledge': result.resultType === 'KNOWLEDGE_ITEM' }"
              >
                <div class="search-result-index">{{ String(index + 1).padStart(2, '0') }}</div>
                <div class="search-result-content">
                  <div class="search-result-meta">
                    <span>{{ result.resultType === 'KNOWLEDGE_ITEM' ? '知识条目' : '资料' }}</span>
                    <span v-if="result.itemType">{{ knowledgeTypeLabel(result.itemType) }}</span>
                    <span v-else>{{ materialTypeLabel(result.materialType) }}</span>
                    <span>{{ result.chapterTitle || '未关联章节' }}</span>
                    <span v-if="result.matchedPageNo">第 {{ result.matchedPageNo }} 页</span>
                    <span v-if="result.key">重点</span>
                    <span v-if="result.importanceLevel">重要度 {{ result.importanceLevel }}</span>
                  </div>
                  <h3>{{ result.title }}</h3>
                  <p>{{ result.matchedSnippet || result.summary || '命中该资料' }}</p>
                  <small>
                    {{ matchSourceLabel(result.matchSource) }}
                    <template v-if="result.originalName"> · {{ result.originalName }}</template>
                  </small>
                </div>
                <div class="search-result-actions">
                  <button
                    v-if="result.resultType === 'KNOWLEDGE_ITEM'"
                    type="button"
                    @click="openKnowledgeResult"
                  >
                    前往知识库
                  </button>
                  <button v-else type="button" @click="editSearchResult(result)">查看资料</button>
                  <button
                    v-if="result.materialId && result.parsedChunkCount > 0"
                    type="button"
                    @click="previewSearchResult(result)"
                  >
                    查看解析文本 ↗
                  </button>
                </div>
              </article>

              <div v-if="!searchLoading && searchHasRun && searchResults.length === 0" class="search-empty">
                <strong>没有找到匹配内容。</strong>
                <p>试试更短的关键词，或清空章节、类型和重点筛选。</p>
              </div>
              <div v-if="!searchLoading && !searchHasRun" class="search-empty">
                <strong>输入一个关键词开始。</strong>
                <p>例如“二叉树遍历”“实验报告”或“2024 期末”。</p>
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
  searchLoading,
  searchRecordLoading,
  searchHasRun,
  chapters,
  searchResults,
  searchRecords,
  materialTypeOptions,
  searchForm,
  loadSearchRecords,
  formatSearchTime,
  searchTypeLabel,
  knowledgeTypeLabel,
  runSearch,
  resetSearch,
  rerunSearchRecord,
  materialTypeLabel,
  matchSourceLabel
} = useDashboardContext()
</script>
