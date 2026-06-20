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
                <button type="button" class="outline-pill danger-pill" @click="confirmCourseDeletion">
                  删除课程
                </button>
              </div>
            </div>
            <div class="hero-stats">
              <div>
                <strong>{{ String(chapters.length).padStart(2, '0') }}</strong>
                <span>chapters</span>
              </div>
              <div>
                <strong>{{ String(overviewStats.materialCount).padStart(2, '0') }}</strong>
                <span>materials</span>
              </div>
              <div>
                <strong>{{ overviewStats.parsedMaterialCount }}</strong>
                <span>parsed</span>
              </div>
              <div>
                <strong>{{ overviewStats.knowledgeItemCount }}</strong>
                <span>knowledge</span>
              </div>
              <div>
                <strong>{{ overviewStats.examQuestionCount }}</strong>
                <span>questions</span>
              </div>
              <div>
                <strong>{{ overviewStats.examMappingCount }}</strong>
                <span>mappings</span>
              </div>
              <div>
                <strong>{{ overviewStats.exportCount }}</strong>
                <span>exports</span>
              </div>
            </div>
            <div v-loading="courseStatsLoading" class="course-type-distribution">
              <div class="type-distribution-heading">
                <strong>资料类型分布</strong>
                <span>{{ overviewStats.materialCount }} 份资料</span>
              </div>
              <div class="type-distribution-list">
                <div v-for="item in materialTypeDistribution" :key="item.type" class="type-distribution-row">
                  <span>{{ item.label }}</span>
                  <div>
                    <i :style="{ width: `${item.percent}%` }"></i>
                  </div>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>
            </div>
          </section>

          <section
            id="relations"
            class="relation-section scroll-panel"
            :class="{ 'section-active': activeSection === 'relations' }"
          >
            <div class="relation-heading">
              <div>
                <p class="eyebrow">课程关系</p>
                <h2>把前置课、关联课和后续课串起来<span>.</span></h2>
              </div>
              <p>课程关系会用于后续关联导出、知识缺口检测和跨课程复习规划。</p>
            </div>

            <div class="relation-panel">
              <div class="relation-form">
                <label>
                  <span>关联课程</span>
                  <el-select v-model="relationForm.relatedCourseId" filterable placeholder="选择一门课程">
                    <el-option
                      v-for="course in relationCandidateCourses"
                      :key="course.id"
                      :label="`${course.courseName} / ${course.courseCode || '未设置编号'}`"
                      :value="course.id"
                    />
                  </el-select>
                </label>
                <label>
                  <span>关系类型</span>
                  <el-select v-model="relationForm.relationType">
                    <el-option
                      v-for="option in relationTypeOptions"
                      :key="option.type"
                      :label="option.label"
                      :value="option.type"
                    />
                  </el-select>
                </label>
                <label>
                  <span>关系说明</span>
                  <el-input v-model="relationForm.reason" maxlength="255" placeholder="例如：学习本课前建议先掌握基础语法" />
                </label>
                <button type="button" :disabled="relationSaving" @click="saveRelation">
                  {{ relationSaving ? '保存中…' : '添加关系' }}
                </button>
              </div>

              <div v-loading="relationLoading" class="relation-groups">
                <section v-for="group in relationGroups" :key="group.type" class="relation-group">
                  <div class="relation-group-heading">
                    <strong>{{ group.label }}</strong>
                    <span>{{ group.items.length }} 门</span>
                  </div>
                  <article v-for="relation in group.items" :key="relation.id" class="relation-card">
                    <div>
                      <strong>{{ relation.relatedCourseName }}</strong>
                      <p>{{ relation.relatedCourseCode || '未设置编号' }} / {{ relation.relatedSemester || '未设置学期' }}</p>
                      <small>{{ relation.reason || '暂无关系说明' }}</small>
                    </div>
                    <button type="button" @click="removeRelation(relation)">删除</button>
                  </article>
                  <div v-if="group.items.length === 0" class="relation-empty">
                    暂无{{ group.label }}。
                  </div>
                </section>
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
                <div class="chapter-card-actions">
                  <button type="button" @click="openChapterEditor(chapter)">edit ↗</button>
                  <button type="button" class="danger-text" @click="confirmChapterDeletion(chapter)">
                    delete
                  </button>
                </div>
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

          <section
            id="search"
            class="search-section scroll-panel"
            :class="{ 'section-active': activeSection === 'search' }"
          >
            <div class="search-heading">
              <div>
                <p class="eyebrow">course search</p>
                <h2>找到你记得的<br>那一小段<span>.</span></h2>
              </div>
              <p>统一检索资料标题、摘要、标签、PDF 正文，以及 AI 整理出的知识条目。</p>
            </div>

            <div class="search-panel">
              <div class="search-input-row">
                <el-input
                  v-model="searchForm.keyword"
                  clearable
                  maxlength="255"
                  placeholder="输入知识点、标签、题目关键词或资料名称"
                  @keyup.enter="runSearch"
                />
                <button type="button" :disabled="searchLoading" @click="runSearch">
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

          <section
            id="knowledge"
            class="knowledge-section scroll-panel"
            :class="{ 'section-active': activeSection === 'knowledge' }"
          >
            <div class="knowledge-heading">
              <div>
                <p class="eyebrow light">ai knowledge studio</p>
                <h2>不是摘词，<br>是理解后整理<span>.</span></h2>
              </div>
              <p>选择一份已经解析的 PDF，让 DeepSeek 按语义整理定义、重点、方法、公式、例子与易错点。</p>
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
                <div class="knowledge-mastery">
                  <div class="knowledge-mastery-row">
                    <label>
                      <span>掌握状态</span>
                      <el-select v-model="item.masteryStatus" placeholder="选择状态">
                        <el-option
                          v-for="option in masteryStatusOptions"
                          :key="option.status"
                          :label="option.label"
                          :value="option.status"
                        />
                      </el-select>
                    </label>
                    <label>
                      <span>掌握分数</span>
                      <el-input-number
                        v-model="item.masteryScore"
                        :min="0"
                        :max="100"
                        :step="5"
                        controls-position="right"
                      />
                    </label>
                  </div>
                  <el-input
                    v-model="item.masteryNote"
                    type="textarea"
                    :rows="2"
                    maxlength="300"
                    show-word-limit
                    placeholder="记录薄弱点、复习提醒或解题问题"
                  />
                  <div class="knowledge-mastery-actions">
                    <span>最近复习：{{ formatReviewTime(item.lastReviewTime) }}</span>
                    <button
                      type="button"
                      :disabled="isMasterySaving(item.id)"
                      @click="saveKnowledgeMastery(item)"
                    >
                      {{ isMasterySaving(item.id) ? '保存中…' : '保存掌握状态' }}
                    </button>
                  </div>
                </div>
                <button type="button" class="danger-text" @click="confirmKnowledgeDeletion(item)">
                  删除条目
                </button>
              </article>
              <div v-if="!knowledgeLoading && knowledgeItems.length === 0" class="knowledge-empty">
                <strong>还没有知识条目。</strong>
                <p>先解析一份 PDF，再让 AI 从完整语义中整理知识。</p>
              </div>
            </div>
          </section>

          <section
            id="gaps"
            class="gap-section scroll-panel"
            :class="{ 'section-active': activeSection === 'gaps' }"
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
                      <span v-if="item.masteryScore !== null">分数 {{ item.masteryScore }}</span>
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

          <section
            id="teacher"
            class="teacher-section scroll-panel"
            :class="{ 'section-active': activeSection === 'teacher' }"
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

          <section
            id="review"
            class="review-section scroll-panel"
            :class="{ 'section-active': activeSection === 'review' }"
          >
            <div class="review-heading">
              <div>
                <p class="eyebrow">复习配置</p>
                <h2>定义生成目标<span>.</span></h2>
              </div>
              <p>保存考试目标、难度、输出类型、前置课程、教师画像和自定义要求，后续可直接用于复习资料生成。</p>
            </div>

            <div class="review-layout">
              <form class="review-form" @submit.prevent="saveReviewProfile">
                <label>
                  <span>配置名称</span>
                  <el-input v-model="reviewProfileForm.profileName" maxlength="128" />
                </label>
                <label>
                  <span>考试目标</span>
                  <el-input v-model="reviewProfileForm.target" maxlength="128" placeholder="例如：期末考试" />
                </label>
                <label>
                  <span>复习难度</span>
                  <el-select v-model="reviewProfileForm.difficultyLevel">
                    <el-option
                      v-for="option in reviewDifficultyOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
                <label>
                  <span>输出类型</span>
                  <el-select v-model="reviewProfileForm.outputType">
                    <el-option
                      v-for="option in reviewOutputOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
                <label>
                  <span>教师画像</span>
                  <el-select v-model="reviewProfileForm.teacherProfileId" clearable placeholder="不绑定画像">
                    <el-option
                      v-for="profile in teacherProfiles"
                      :key="profile.id"
                      :label="profile.teacherName"
                      :value="profile.id"
                    />
                  </el-select>
                </label>
                <label class="review-switch">
                  <el-switch v-model="reviewProfileForm.includePrerequisites" />
                  <span>包含前置课程</span>
                </label>
                <label class="review-wide">
                  <span>自定义要求</span>
                  <el-input
                    v-model="reviewProfileForm.customRequirement"
                    type="textarea"
                    :rows="4"
                    placeholder="例如：重点覆盖树、图和排序，输出适合考前两天冲刺的版本"
                  />
                </label>
                <div class="review-form-actions">
                  <button v-if="editingReviewProfileId" type="button" @click="resetReviewProfileForm">
                    取消编辑
                  </button>
                  <button type="submit" :disabled="reviewProfileSaving">
                    {{ reviewProfileSaving ? '保存中…' : editingReviewProfileId ? '保存修改' : '创建配置' }}
                  </button>
                </div>
              </form>

              <div v-loading="reviewProfileLoading" class="review-profile-list">
                <article v-for="profile in reviewProfiles" :key="profile.id" class="review-profile-card">
                  <div class="review-card-top">
                    <span>{{ reviewOutputLabel(profile.outputType) }}</span>
                    <strong>{{ reviewDifficultyLabel(profile.difficultyLevel) }}</strong>
                  </div>
                  <h3>{{ profile.profileName }}</h3>
                  <p>{{ profile.customRequirement || '暂无自定义要求' }}</p>
                  <div class="review-card-meta">
                    <span>{{ profile.target || '未设置目标' }}</span>
                    <span>{{ profile.includePrerequisites ? '包含前置课程' : '仅当前课程' }}</span>
                    <span>{{ profile.teacherName || '未绑定教师画像' }}</span>
                  </div>
                  <div class="review-card-actions">
                    <button type="button" @click="editReviewProfile(profile)">编辑</button>
                    <button type="button" @click="removeReviewProfile(profile)">删除</button>
                  </div>
                </article>
                <div
                  v-if="!reviewProfileLoading && reviewProfiles.length === 0"
                  class="review-empty"
                >
                  <strong>还没有复习配置。</strong>
                  <p>先创建一个目标，后续生成复习资料时可以复用。</p>
                </div>
              </div>
            </div>
          </section>

          <section
            id="ai-config"
            class="ai-config-section scroll-panel"
            :class="{ 'section-active': activeSection === 'ai-config' }"
          >
            <div class="ai-config-heading">
              <div>
                <p class="eyebrow">AI 配置</p>
                <h2>管理模型供应商<span>.</span></h2>
              </div>
              <p>保存供应商、Base URL、模型名称和 API Key 环境变量别名。系统不会保存明文 API Key。</p>
            </div>

            <div class="ai-config-status">
              <div>
                <span>默认 DeepSeek</span>
                <strong>{{ aiDefaultStatus?.configured ? '已配置' : '未配置' }}</strong>
              </div>
              <div>
                <span>默认模型</span>
                <strong>{{ aiDefaultStatus?.defaultModel || '未读取' }}</strong>
              </div>
              <div>
                <span>配置数量</span>
                <strong>{{ aiProviders.length }}</strong>
              </div>
            </div>

            <div class="ai-config-layout">
              <form class="ai-provider-form" @submit.prevent="saveAiProvider">
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
                  <el-input v-model="aiProviderForm.apiKeyAlias" maxlength="128" placeholder="例如：DEEPSEEK_API_KEY" />
                </label>
                <label class="ai-provider-switch">
                  <el-switch v-model="aiProviderForm.enabled" />
                  <span>启用配置</span>
                </label>
                <p>这里只保存环境变量名，不保存 API Key 明文。</p>
                <div class="ai-provider-actions">
                  <button v-if="editingAiProviderId" type="button" @click="resetAiProviderForm">
                    取消编辑
                  </button>
                  <button type="submit" :disabled="aiProviderSaving">
                    {{ aiProviderSaving ? '保存中…' : editingAiProviderId ? '保存修改' : '创建配置' }}
                  </button>
                </div>
              </form>

              <div v-loading="aiProviderLoading" class="ai-provider-list">
                <article v-for="config in aiProviders" :key="config.id" class="ai-provider-card">
                  <div class="ai-provider-card-top">
                    <span>{{ config.enabled ? '已启用' : '已停用' }}</span>
                    <strong>{{ config.providerName }}</strong>
                  </div>
                  <h3>{{ config.modelName }}</h3>
                  <p>{{ config.baseUrl }}</p>
                  <div class="ai-provider-meta">
                    <span>{{ config.apiKeyAlias || '未设置 Key 别名' }}</span>
                  </div>
                  <div class="ai-provider-card-actions">
                    <button type="button" @click="editAiProvider(config)">编辑</button>
                    <button type="button" @click="removeAiProvider(config)">删除</button>
                  </div>
                </article>
                <div v-if="!aiProviderLoading && aiProviders.length === 0" class="ai-provider-empty">
                  <strong>还没有自定义 AI 配置。</strong>
                  <p>默认 DeepSeek 环境变量配置仍然可用。</p>
                </div>
              </div>
            </div>
          </section>

          <section
            id="ai-tasks"
            class="ai-task-section scroll-panel"
            :class="{ 'section-active': activeSection === 'ai-tasks' }"
          >
            <div class="ai-task-heading">
              <div>
                <p class="eyebrow">AI 任务记录</p>
                <h2>追踪生成任务<span>.</span></h2>
              </div>
              <p>保存教师画像、知识整理和后续复习生成任务的提示词、状态、结果路径和失败原因，便于排查与复用。</p>
            </div>

            <div class="ai-task-status">
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
              <button type="button" :disabled="aiTaskLoading" @click="loadAiGenerationTasks">
                {{ aiTaskLoading ? '刷新中…' : '刷新记录' }}
              </button>
            </div>

            <div v-loading="aiTaskLoading" class="ai-task-list">
              <article v-for="task in aiGenerationTasks" :key="task.id" class="ai-task-card">
                <div class="ai-task-card-top">
                  <span :class="`ai-task-status-${task.status?.toLowerCase() || 'pending'}`">
                    {{ aiTaskStatusLabel(task.status) }}
                  </span>
                  <strong>#{{ task.id }}</strong>
                </div>
                <h3>{{ aiTaskTypeLabel(task.taskType) }}</h3>
                <p>{{ task.prompt || '未保存提示词' }}</p>
                <div class="ai-task-meta">
                  <span>{{ formatAiTaskTime(task.createTime) }}</span>
                  <span v-if="task.finishTime">完成：{{ formatAiTaskTime(task.finishTime) }}</span>
                  <span v-if="task.resultPath">{{ task.resultPath }}</span>
                </div>
                <small v-if="task.errorMessage">{{ task.errorMessage }}</small>
              </article>

              <div v-if="!aiTaskLoading && aiGenerationTasks.length === 0" class="ai-task-empty">
                <strong>还没有 AI 生成任务。</strong>
                <p>运行知识整理或教师画像分析后，这里会记录任务状态。</p>
              </div>
            </div>
          </section>

          <section
            id="exam"
            class="exam-section scroll-panel"
            :class="{ 'section-active': activeSection === 'exam' }"
          >
            <ExamMappingPanel
              :selected-course="selectedCourse"
              :current-user-id="currentUser?.id"
              :materials="materials"
              :chapters="chapters"
              :knowledge-items="knowledgeItems"
              :preferred-material-id="examPreferredMaterialId"
              @material-parsed="handleExamMaterialParsed"
              @stats-changed="loadCourseStats"
            />
          </section>

          <section
            id="export"
            class="export-section scroll-panel"
            :class="{ 'section-active': activeSection === 'export' }"
          >
            <div class="export-heading">
              <div>
                <p class="eyebrow">知识包导出</p>
                <h2>导出给 AI 使用的课程知识包<span>.</span></h2>
              </div>
              <p>把课程资料、标签、知识条目、章节结构和考点统计整理成 ZIP，保存导出记录并随时下载。</p>
            </div>

            <div class="export-panel">
              <div class="export-form-grid">
                <label class="export-field">
                  <span>导出名称</span>
                  <el-input v-model="exportForm.exportName" maxlength="255" placeholder="例如：期末复习知识包" />
                </label>
                <label class="export-field">
                  <span>导出模板</span>
                  <el-select v-model="exportForm.templateId" placeholder="选择模板">
                    <el-option
                      v-for="template in exportTemplates"
                      :key="template.id"
                      :label="`${template.templateName} / ${template.targetAgent}`"
                      :value="template.id"
                    />
                  </el-select>
                </label>
                <label class="export-field">
                  <span>章节范围</span>
                  <el-select v-model="exportForm.chapterIds" multiple collapse-tags clearable placeholder="全部章节">
                    <el-option
                      v-for="chapter in chapters"
                      :key="chapter.id"
                      :label="`${chapter.chapterNo} ${chapter.chapterTitle}`"
                      :value="chapter.id"
                    />
                  </el-select>
                </label>
                <label class="export-field">
                  <span>资料类型</span>
                  <el-select v-model="exportForm.materialTypes" multiple collapse-tags clearable placeholder="全部类型">
                    <el-option
                      v-for="option in materialTypeOptions"
                      :key="option.type"
                      :label="option.label"
                      :value="option.type"
                    />
                  </el-select>
                </label>
              </div>

              <div class="export-options">
                <label>
                  <el-switch v-model="exportForm.onlyKeyMaterials" />
                  <span>只导出重点资料</span>
                </label>
                <label>
                  <el-switch v-model="exportForm.includeExamStats" />
                  <span>包含高频考点统计</span>
                </label>
                <label
                  v-for="option in exportRelationOptions"
                  :key="option.type"
                  :class="{ 'option-disabled': option.count === 0 }"
                >
                  <el-switch
                    v-model="exportForm[option.exportField]"
                    :disabled="option.count === 0"
                  />
                  <span>包含{{ option.label }}重点内容</span>
                  <small>{{ option.count }} 门</small>
                </label>
                <button
                  type="button"
                  class="export-preview-button"
                  :disabled="exportPreviewLoading || exportCreating"
                  @click="runExportPreview"
                >
                  <span>{{ exportPreviewLoading ? '预览中…' : '预览内容' }}</span>
                  <strong>?</strong>
                </button>
                <button type="button" :disabled="exportCreating" @click="runExport">
                  <span>{{ exportCreating ? '导出中…' : '生成知识包' }}</span>
                  <strong>→</strong>
                </button>
              </div>

              <div v-loading="exportPreviewLoading" class="export-preview">
                <template v-if="exportPreview">
                  <div class="export-preview-heading">
                    <div>
                      <span>预览范围</span>
                      <strong>{{ exportPreview.courseName }}</strong>
                    </div>
                    <p>{{ exportPreview.templateName || '默认模板' }} · {{ exportPreview.exportFormat }}</p>
                  </div>

                  <div class="export-preview-stats">
                    <div>
                      <span>章节</span>
                      <strong>{{ exportPreview.summary.chapterCount }}</strong>
                    </div>
                    <div>
                      <span>资料</span>
                      <strong>{{ exportPreview.summary.materialCount }}</strong>
                    </div>
                    <div>
                      <span>已解析</span>
                      <strong>{{ exportPreview.summary.parsedMaterialCount }}</strong>
                    </div>
                    <div>
                      <span>知识条目</span>
                      <strong>{{ exportPreview.summary.knowledgeItemCount }}</strong>
                    </div>
                    <div>
                      <span>考点统计</span>
                      <strong>{{ exportPreview.summary.examStatCount }}</strong>
                    </div>
                    <div>
                      <span>关联课程</span>
                      <strong>{{ exportPreview.summary.relatedCourseCount }}</strong>
                    </div>
                  </div>

                  <div class="export-preview-grid">
                    <section>
                      <h3>课程章节</h3>
                      <ul>
                        <li v-for="chapter in exportPreview.chapters" :key="chapter.id">
                          {{ chapter.chapterNo }} {{ chapter.chapterTitle }}
                        </li>
                      </ul>
                      <p v-if="exportPreview.chapters.length === 0">未包含章节结构。</p>
                    </section>

                    <section>
                      <h3>课程资料</h3>
                      <ul>
                        <li v-for="material in exportPreview.materials" :key="material.id">
                          <strong>{{ material.title }}</strong>
                          <span>{{ materialTypeLabel(material.materialType) }} · {{ material.parsedChunkCount }} 块文本</span>
                        </li>
                      </ul>
                      <p v-if="exportPreview.materials.length === 0">当前筛选条件下没有资料。</p>
                    </section>

                    <section>
                      <h3>知识条目</h3>
                      <ul>
                        <li v-for="item in exportPreview.knowledgeItems" :key="item.id">
                          <strong>{{ item.title }}</strong>
                          <span>{{ knowledgeTypeLabel(item.itemType) }} · 重要度 {{ item.importanceLevel || '-' }}</span>
                        </li>
                      </ul>
                      <p v-if="exportPreview.knowledgeItems.length === 0">当前筛选条件下没有知识条目。</p>
                    </section>

                    <section>
                      <h3>考点统计</h3>
                      <ul>
                        <li v-for="stat in exportPreview.examStats" :key="stat.knowledgeItemId || stat.knowledgeTitle">
                          <strong>{{ stat.knowledgeTitle }}</strong>
                          <span>{{ stat.questionCount }} 题 · {{ stat.totalScore || 0 }} 分 · {{ stat.latestExamYear || '未知年份' }}</span>
                        </li>
                      </ul>
                      <p v-if="exportPreview.examStats.length === 0">未包含考点统计。</p>
                    </section>
                  </div>

                  <div class="export-preview-related">
                    <div class="export-preview-related-title">
                      <strong>{{ exportPreview.summary.relatedMaterialCount }}</strong>
                      <span>关联课程重点资料</span>
                      <strong>{{ exportPreview.summary.relatedKnowledgeItemCount }}</strong>
                      <span>关联课程知识条目</span>
                    </div>
                    <article v-for="course in exportPreview.relatedCourses" :key="course.courseId">
                      <div>
                        <span>{{ relationTypeLabel(course.relationType) }}</span>
                        <h3>{{ course.courseName }}</h3>
                        <p>{{ course.chapterCount }} 个章节 · {{ course.materials.length }} 份重点资料 · {{ course.knowledgeItems.length }} 条知识</p>
                      </div>
                      <ul>
                        <li v-for="material in course.materials" :key="material.id">{{ material.title }}</li>
                      </ul>
                    </article>
                    <p v-if="exportPreview.relatedCourses.length === 0">未包含关联课程内容。</p>
                  </div>
                </template>
                <div v-else class="export-preview-empty">
                  <strong>先预览，再导出。</strong>
                  <p>预览会按当前筛选条件展示 ZIP 将包含的课程结构、资料、知识、考点统计和关联课程内容。</p>
                </div>
              </div>
            </div>

            <div class="export-records">
              <div class="export-record-heading">
                <div>
                  <strong>{{ exportRecords.length }}</strong>
                  <span>导出记录</span>
                </div>
                <button type="button" :disabled="exportLoading" @click="loadExportData">刷新记录</button>
              </div>

              <div v-loading="exportLoading" class="export-record-grid">
                <article v-for="record in exportRecords" :key="record.id" class="export-record-card">
                  <div>
                    <span>{{ record.exportFormat }}</span>
                    <h3>{{ record.exportName }}</h3>
                    <p>{{ formatExportTime(record.exportTime) }}</p>
                  </div>
                  <button type="button" @click="downloadExport(record)">下载 ZIP</button>
                </article>
                <div v-if="!exportLoading && exportRecords.length === 0" class="export-empty">
                  <strong>还没有导出记录。</strong>
                  <p>生成第一个课程知识包后，这里会保存记录和下载入口。</p>
                </div>
              </div>
            </div>
          </section>
        </template>

        <section v-else class="no-course">
          <div class="no-course-shape"></div>
          <p class="eyebrow">第一门课程</p>
          <h1>从一门课程开始<span>.</span></h1>
          <button type="button" class="primary-pill" @click="openCourseCreator">新建课程 ＋</button>
        </section>
      </section>
    </div>
  </main>

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

  <el-dialog
    v-model="textPreviewVisible"
    class="studio-dialog studio-dialog-text"
    modal-class="studio-dialog-overlay"
    width="880px"
    :show-close="false"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>parsed text / 解析文本</p>
          <h2>{{ previewMaterial?.title || '资料文本' }}<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="textPreviewVisible = false">
          ×
        </button>
      </div>
    </template>
    <div v-loading="textChunkLoading" class="text-preview">
      <section v-for="(chunk, index) in textChunks" :key="chunk.id" class="text-page">
        <div class="text-page-marker">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
        </div>
        <div class="text-page-content">
          <div class="text-page-heading">
            <strong>第 {{ chunk.pageNo || index + 1 }} 页</strong>
            <span>{{ chunk.wordCount || chunk.content?.length || 0 }} 字符</span>
          </div>
          <div class="text-page-body">{{ chunk.content }}</div>
        </div>
      </section>
      <el-empty v-if="!textChunkLoading && textChunks.length === 0" description="暂无解析文本" />
    </div>
    <template #footer>
      <div class="dialog-footer text-dialog-footer">
        <span>共 {{ textChunks.length }} 个文本块</span>
        <button type="button" class="dialog-button dialog-button-primary" @click="textPreviewVisible = false">
          <span>阅读完成</span>
          <strong>→</strong>
        </button>
      </div>
    </template>
  </el-dialog>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ExamMappingPanel from '../../components/dashboard/ExamMappingPanel.vue'
import { login, register } from '../../api/auth'
import {
  createChapter,
  createCourse,
  createCourseRelation,
  deleteChapter,
  deleteCourse,
  deleteCourseRelation,
  getCourseStats,
  listChapters,
  listCourses,
  listCourseRelations,
  updateChapter,
  updateCourse
} from '../../api/course'
import {
  deleteMaterial,
  generateMaterialSummary,
  listSimilarMaterials,
  listMaterials,
  listTextChunks,
  parsePdf,
  updateMaterial,
  uploadMaterialsBatch
} from '../../api/material'
import { listSearchRecords, searchMaterials } from '../../api/search'
import {
  deleteKnowledgeItem,
  generateKnowledgeItems,
  updateKnowledgeMastery,
  listCourseTags,
  listKnowledgeItems,
  listMaterialTags,
  previewMaterialTags,
  replaceMaterialTags
} from '../../api/knowledge'
import {
  createExport,
  exportDownloadUrl,
  listExportRecords,
  listExportTemplates,
  previewExport
} from '../../api/export'
import {
  createKnowledgeGapReport,
  listKnowledgeGapItems,
  listKnowledgeGapReports
} from '../../api/gap'
import {
  analyzeTeacherProfile,
  listTeacherProfileEvidence,
  listTeacherProfiles,
  updateTeacherProfile
} from '../../api/teacher'
import {
  createReviewProfile,
  deleteReviewProfile,
  listReviewProfiles,
  updateReviewProfile
} from '../../api/review'
import {
  createAiProvider,
  deleteAiProvider,
  getAiStatus,
  listAiGenerationTasks,
  listAiProviders,
  updateAiProvider
} from '../../api/ai'

const currentUser = ref(null)
const authMode = ref('login')
const authLoading = ref(false)
const courseLoading = ref(false)
const courseStatsLoading = ref(false)
const chapterLoading = ref(false)
const materialLoading = ref(false)
const searchLoading = ref(false)
const searchRecordLoading = ref(false)
const searchHasRun = ref(false)
const relationLoading = ref(false)
const relationSaving = ref(false)
const knowledgeLoading = ref(false)
const knowledgeGenerating = ref(false)
const gapLoading = ref(false)
const gapGenerating = ref(false)
const gapItemLoading = ref(false)
const teacherProfileLoading = ref(false)
const teacherProfileAnalyzing = ref(false)
const teacherEvidenceLoading = ref(false)
const teacherProfileEditing = ref(false)
const teacherProfileSaving = ref(false)
const reviewProfileLoading = ref(false)
const reviewProfileSaving = ref(false)
const aiProviderLoading = ref(false)
const aiProviderSaving = ref(false)
const aiTaskLoading = ref(false)
const tagSaving = ref(false)
const tagPreviewLoading = ref(false)
const exportLoading = ref(false)
const exportCreating = ref(false)
const exportPreviewLoading = ref(false)
const courseSaving = ref(false)
const chapterSaving = ref(false)
const materialSaving = ref(false)
const parsingMaterialId = ref(null)
const summaryGeneratingIds = ref([])
const textChunkLoading = ref(false)
const courseDialogVisible = ref(false)
const chapterDialogVisible = ref(false)
const materialDialogVisible = ref(false)
const textPreviewVisible = ref(false)
const similarDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const deleteLoading = ref(false)
const deleteTarget = ref(null)
const editingCourseId = ref(null)
const editingChapterId = ref(null)
const editingMaterialId = ref(null)
const courses = ref([])
const chapters = ref([])
const materials = ref([])
const courseRelations = ref([])
const courseStats = ref(null)
const searchResults = ref([])
const searchRecords = ref([])
const knowledgeItems = ref([])
const gapReports = ref([])
const gapItems = ref([])
const teacherProfiles = ref([])
const teacherProfileEvidence = ref([])
const reviewProfiles = ref([])
const aiProviders = ref([])
const aiGenerationTasks = ref([])
const aiDefaultStatus = ref(null)
const courseTags = ref([])
const exportTemplates = ref([])
const exportRecords = ref([])
const exportPreview = ref(null)
const masterySavingIds = ref([])
const selectedMaterialTags = ref([])
const previewedMaterialTags = ref([])
const knowledgeFilterType = ref(null)
const selectedCourse = ref(null)
const selectedGapReport = ref(null)
const selectedTeacherProfile = ref(null)
const editingReviewProfileId = ref(null)
const editingAiProviderId = ref(null)
const uploadFileList = ref([])
const uploadDrafts = ref([])
const materialUploadRef = ref(null)
const previewMaterial = ref(null)
const similarSourceMaterial = ref(null)
const similarMaterialResults = ref([])
const textChunks = ref([])
const activeSection = ref('overview')
const examPreferredMaterialId = ref(null)

const pageSections = [
  { id: 'overview', label: '概览', title: '课程概览' },
  { id: 'relations', label: '关系', title: '课程关系' },
  { id: 'chapters', label: '章节', title: '章节路径' },
  { id: 'materials', label: '资料', title: '课程资料' },
  { id: 'search', label: '检索', title: '课程检索' },
  { id: 'knowledge', label: '知识', title: '知识条目' },
  { id: 'gaps', label: '缺口', title: '知识缺口' },
  { id: 'teacher', label: '画像', title: '教师画像' },
  { id: 'review', label: '复习', title: '复习配置' },
  { id: 'ai-config', label: 'AI', title: 'AI 配置' },
  { id: 'ai-tasks', label: '任务', title: 'AI 任务记录' },
  { id: 'exam', label: '真题', title: '真题映射' },
  { id: 'export', label: '导出', title: '知识包导出' }
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

const aiTaskStats = computed(() => ({
  total: aiGenerationTasks.value.length,
  running: aiGenerationTasks.value.filter(task => task.status === 'RUNNING' || task.status === 'PENDING').length,
  failed: aiGenerationTasks.value.filter(task => task.status === 'FAILED').length
}))

const overviewStats = computed(() => ({
  materialCount: courseStats.value?.materialCount ?? materials.value.length,
  parsedMaterialCount: courseStats.value?.parsedMaterialCount
    ?? materials.value.filter(item => item.parsedChunkCount > 0).length,
  knowledgeItemCount: courseStats.value?.knowledgeItemCount ?? knowledgeItems.value.length,
  examQuestionCount: courseStats.value?.examQuestionCount ?? 0,
  examMappingCount: courseStats.value?.examMappingCount ?? 0,
  exportCount: courseStats.value?.exportCount ?? exportRecords.value.length
}))

const materialTypeDistribution = computed(() => {
  const stats = courseStats.value?.materialTypeStats || []
  return materialTypeOptions.map(option => {
    const match = stats.find(item => item.materialType === option.type)
    const count = match?.count || 0
    const total = overviewStats.value.materialCount || 0
    return {
      ...option,
      count,
      percent: total > 0 ? Math.round((count / total) * 100) : 0
    }
  })
})

const parsedMaterials = computed(() => materials.value.filter(
  material => material.parsedChunkCount > 0
))

const knowledgeTypeOptions = [
  { type: 'DEFINITION', label: '定义' },
  { type: 'KEY_POINT', label: '重点' },
  { type: 'FORMULA', label: '公式' },
  { type: 'METHOD', label: '方法' },
  { type: 'EXAMPLE', label: '例子' },
  { type: 'WARNING', label: '易错点' }
]

const masteryStatusOptions = [
  { status: 'UNKNOWN', label: '未评估' },
  { status: 'LEARNING', label: '学习中' },
  { status: 'MASTERED', label: '已掌握' },
  { status: 'WEAK', label: '薄弱' },
  { status: 'NEED_REVIEW', label: '待复习' }
]

const reviewDifficultyOptions = [
  { value: 'EASY', label: '基础' },
  { value: 'MEDIUM', label: '标准' },
  { value: 'MEDIUM_HARD', label: '偏难' },
  { value: 'HARD', label: '冲刺' }
]

const reviewOutputOptions = [
  { value: 'REVIEW_NOTE', label: '复习笔记' },
  { value: 'OUTLINE', label: '复习提纲' },
  { value: 'FLASHCARDS', label: '记忆卡片' },
  { value: 'MOCK_EXAM', label: '模拟题' },
  { value: 'CHECKLIST', label: '检查清单' }
]

const relationTypeOptions = [
  { type: 'PREREQUISITE', label: '前置课程', exportField: 'includePrerequisiteCourses' },
  { type: 'RELATED', label: '关联课程', exportField: 'includeRelatedCourses' },
  { type: 'FOLLOW_UP', label: '后续课程', exportField: 'includeFollowUpCourses' }
]

const relationCandidateCourses = computed(() => {
  const relatedIds = new Set(courseRelations.value.map(item => item.relatedCourseId))
  return courses.value.filter(course => (
    selectedCourse.value
    && course.id !== selectedCourse.value.id
    && !relatedIds.has(course.id)
  ))
})

const relationGroups = computed(() => relationTypeOptions.map(option => ({
  ...option,
  items: courseRelations.value.filter(item => item.relationType === option.type)
})))

const exportRelationOptions = computed(() => relationTypeOptions.map(option => ({
  ...option,
  count: courseRelations.value.filter(item => item.relationType === option.type).length
})))

const gapStats = computed(() => ({
  total: gapItems.value.length,
  severe: gapItems.value.filter(item => item.severityLevel >= 4).length,
  prerequisite: gapItems.value.filter(item => item.relatedCourseRelationId).length
}))

const teacherProfileMaterials = computed(() => materials.value.filter(material => (
  material.parsedChunkCount > 0 || material.materialType === 'EXAM'
)))

const deleteDialogTitle = computed(() => {
  if (deleteTarget.value?.type === 'course') return '删除这门课程'
  if (deleteTarget.value?.type === 'chapter') return '删除这个章节'
  if (deleteTarget.value?.type === 'knowledge') return '删除这条知识'
  return '删除这份资料'
})

const deleteDialogDescription = computed(() => {
  if (deleteTarget.value?.type === 'course') {
    return '课程内的章节、资料、解析文本和相关数据都会一并删除。'
  }
  if (deleteTarget.value?.type === 'chapter') {
    return '章节会被删除，但其中的资料将保留并变为未关联章节。'
  }
  if (deleteTarget.value?.type === 'knowledge') {
    return '该知识条目会从课程知识库中移除，原始资料和解析文本不会受影响。'
  }
  return '原始文件、解析文本和相关数据都会一并删除。'
})

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

const searchForm = reactive({
  keyword: '',
  chapterId: null,
  materialType: null,
  isKey: false
})

const relationForm = reactive({
  relatedCourseId: null,
  relationType: 'PREREQUISITE',
  reason: ''
})

const knowledgeForm = reactive({
  materialId: null,
  maxItems: 12,
  replaceExisting: false
})

const gapForm = reactive({
  reportName: '',
  includePrerequisites: true
})

const teacherProfileForm = reactive({
  teacherName: '',
  materialIds: []
})

const teacherEditForm = reactive({
  teacherName: '',
  confidenceScore: 50,
  examStyle: '',
  questionPreference: '',
  gradingPreference: '',
  focusTopics: '',
  avoidTopics: '',
  sourceSummary: ''
})

const reviewProfileForm = reactive({
  profileName: '',
  target: '',
  difficultyLevel: 'MEDIUM',
  outputType: 'REVIEW_NOTE',
  includePrerequisites: true,
  teacherProfileId: null,
  customRequirement: ''
})

const aiProviderForm = reactive({
  providerName: 'DeepSeek',
  baseUrl: 'https://api.deepseek.com',
  modelName: 'deepseek-v4-flash',
  apiKeyAlias: 'DEEPSEEK_API_KEY',
  enabled: true
})

const exportForm = reactive({
  exportName: '',
  templateId: null,
  chapterIds: [],
  materialTypes: [],
  onlyKeyMaterials: false,
  includeExamStats: true,
  includePrerequisiteCourses: false,
  includeRelatedCourses: false,
  includeFollowUpCourses: false
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
      courseRelations.value = []
      courseStats.value = null
      searchRecords.value = []
      clearGapState()
      clearTeacherProfileState()
      clearReviewProfileState()
      clearAiProviderState()
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
  examPreferredMaterialId.value = null
  resetRelationForm()
  resetGapForm()
  resetTeacherProfileForm()
  resetReviewProfileForm()
  resetAiProviderForm()
  resetSearch()
  knowledgeForm.materialId = null
  selectedMaterialTags.value = []
  previewedMaterialTags.value = []
  knowledgeFilterType.value = null
  chapterLoading.value = true
  materialLoading.value = true
  try {
    const [chapterData, materialData, relationData] = await Promise.all([
      listChapters(course.id, currentUser.value.id),
      listMaterials(course.id, currentUser.value.id),
      listCourseRelations(course.id, currentUser.value.id)
    ])
    chapters.value = chapterData
    materials.value = materialData
    courseRelations.value = relationData
    resetExportForm()
    await Promise.all([
      loadKnowledgeItems(),
      loadCourseStats(),
      loadCourseTags(),
      loadSearchRecords(),
      loadExportData(),
      loadGapReports(),
      loadTeacherProfiles(),
      loadReviewProfiles(),
      loadAiProviders(),
      loadAiGenerationTasks()
    ])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    chapterLoading.value = false
    materialLoading.value = false
  }
}

async function loadCourseRelations() {
  if (!selectedCourse.value) return
  relationLoading.value = true
  try {
    courseRelations.value = await listCourseRelations(selectedCourse.value.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    relationLoading.value = false
  }
}

async function loadCourseStats() {
  if (!selectedCourse.value || !currentUser.value) return
  courseStatsLoading.value = true
  try {
    courseStats.value = await getCourseStats(selectedCourse.value.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    courseStatsLoading.value = false
  }
}

async function loadSearchRecords() {
  if (!selectedCourse.value || !currentUser.value) return
  searchRecordLoading.value = true
  try {
    searchRecords.value = await listSearchRecords(currentUser.value.id, selectedCourse.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    searchRecordLoading.value = false
  }
}

async function saveRelation() {
  if (!relationForm.relatedCourseId) {
    ElMessage.warning('请选择关联课程')
    return
  }
  relationSaving.value = true
  try {
    const relation = await createCourseRelation(
      selectedCourse.value.id,
      currentUser.value.id,
      {
        relatedCourseId: relationForm.relatedCourseId,
        relationType: relationForm.relationType,
        reason: relationForm.reason.trim() || null,
        sortOrder: courseRelations.value.length
      }
    )
    courseRelations.value.push(relation)
    resetRelationForm()
    ElMessage.success('课程关系已添加')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    relationSaving.value = false
  }
}

async function removeRelation(relation) {
  try {
    await deleteCourseRelation(relation.id, currentUser.value.id)
    courseRelations.value = courseRelations.value.filter(item => item.id !== relation.id)
    ElMessage.success('课程关系已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadExportData() {
  if (!selectedCourse.value || !currentUser.value) return
  exportLoading.value = true
  try {
    const [templates, records] = await Promise.all([
      listExportTemplates(),
      listExportRecords(currentUser.value.id, selectedCourse.value.id)
    ])
    exportTemplates.value = templates
    exportRecords.value = records
    if (!exportForm.templateId && templates.length > 0) {
      exportForm.templateId = templates[0].id
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exportLoading.value = false
  }
}

async function loadGapReports() {
  if (!selectedCourse.value || !currentUser.value) return
  gapLoading.value = true
  try {
    gapReports.value = await listKnowledgeGapReports(selectedCourse.value.id, currentUser.value.id)
    if (gapReports.value.length > 0) {
      await selectGapReport(gapReports.value[0])
    } else {
      selectedGapReport.value = null
      gapItems.value = []
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    gapLoading.value = false
  }
}

async function generateGapReport() {
  if (!selectedCourse.value || !currentUser.value) return
  gapGenerating.value = true
  try {
    const report = await createKnowledgeGapReport(selectedCourse.value.id, {
      userId: currentUser.value.id,
      reportName: gapForm.reportName.trim() || null,
      includePrerequisites: gapForm.includePrerequisites
    })
    gapReports.value = [report, ...gapReports.value.filter(item => item.id !== report.id)]
    await selectGapReport(report)
    resetGapForm()
    ElMessage.success('知识缺口报告已生成')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    gapGenerating.value = false
  }
}

async function selectGapReport(report) {
  selectedGapReport.value = report
  gapItemLoading.value = true
  try {
    gapItems.value = await listKnowledgeGapItems(report.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    gapItemLoading.value = false
  }
}

async function loadTeacherProfiles() {
  if (!selectedCourse.value || !currentUser.value) return
  teacherProfileLoading.value = true
  try {
    teacherProfiles.value = await listTeacherProfiles(selectedCourse.value.id, currentUser.value.id)
    if (teacherProfiles.value.length > 0) {
      await selectTeacherProfile(teacherProfiles.value[0])
    } else {
      selectedTeacherProfile.value = null
      teacherProfileEvidence.value = []
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherProfileLoading.value = false
  }
}

async function runTeacherProfileAnalysis() {
  if (!selectedCourse.value || !currentUser.value) return
  if (!teacherProfileForm.teacherName.trim()) {
    ElMessage.warning('请输入教师名称')
    return
  }
  teacherProfileAnalyzing.value = true
  try {
    const profile = await analyzeTeacherProfile(selectedCourse.value.id, {
      userId: currentUser.value.id,
      teacherName: teacherProfileForm.teacherName.trim(),
      materialIds: teacherProfileForm.materialIds
    })
    teacherProfiles.value = [profile, ...teacherProfiles.value.filter(item => item.id !== profile.id)]
    await selectTeacherProfile(profile)
    resetTeacherProfileForm()
    ElMessage.success('教师画像分析完成')
  } catch (error) {
    await loadTeacherProfiles()
    ElMessage.error(error.message)
  } finally {
    await loadAiGenerationTasks()
    teacherProfileAnalyzing.value = false
  }
}

async function selectTeacherProfile(profile) {
  selectedTeacherProfile.value = profile
  teacherProfileEditing.value = false
  teacherEvidenceLoading.value = true
  try {
    teacherProfileEvidence.value = await listTeacherProfileEvidence(profile.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherEvidenceLoading.value = false
  }
}

async function loadReviewProfiles() {
  if (!selectedCourse.value || !currentUser.value) return
  reviewProfileLoading.value = true
  try {
    reviewProfiles.value = await listReviewProfiles(currentUser.value.id, selectedCourse.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reviewProfileLoading.value = false
  }
}

async function saveReviewProfile() {
  if (!selectedCourse.value || !currentUser.value) return
  if (!reviewProfileForm.profileName.trim()) {
    ElMessage.warning('请输入配置名称')
    return
  }
  reviewProfileSaving.value = true
  try {
    const payload = {
      userId: currentUser.value.id,
      courseId: selectedCourse.value.id,
      teacherProfileId: reviewProfileForm.teacherProfileId,
      profileName: reviewProfileForm.profileName.trim(),
      target: reviewProfileForm.target.trim() || null,
      difficultyLevel: reviewProfileForm.difficultyLevel,
      outputType: reviewProfileForm.outputType,
      includePrerequisites: reviewProfileForm.includePrerequisites,
      customRequirement: reviewProfileForm.customRequirement.trim() || null
    }
    const saved = editingReviewProfileId.value
      ? await updateReviewProfile(editingReviewProfileId.value, payload)
      : await createReviewProfile(payload)
    if (editingReviewProfileId.value) {
      replaceItem(reviewProfiles.value, saved)
    } else {
      reviewProfiles.value.unshift(saved)
    }
    resetReviewProfileForm()
    ElMessage.success('复习配置已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reviewProfileSaving.value = false
  }
}

function editReviewProfile(profile) {
  editingReviewProfileId.value = profile.id
  reviewProfileForm.profileName = profile.profileName || ''
  reviewProfileForm.target = profile.target || ''
  reviewProfileForm.difficultyLevel = profile.difficultyLevel || 'MEDIUM'
  reviewProfileForm.outputType = profile.outputType || 'REVIEW_NOTE'
  reviewProfileForm.includePrerequisites = profile.includePrerequisites !== false
  reviewProfileForm.teacherProfileId = profile.teacherProfileId || null
  reviewProfileForm.customRequirement = profile.customRequirement || ''
}

async function removeReviewProfile(profile) {
  try {
    await deleteReviewProfile(profile.id, currentUser.value.id)
    reviewProfiles.value = reviewProfiles.value.filter(item => item.id !== profile.id)
    if (editingReviewProfileId.value === profile.id) {
      resetReviewProfileForm()
    }
    ElMessage.success('复习配置已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

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
  if (!currentUser.value || !selectedCourse.value) return
  aiTaskLoading.value = true
  try {
    aiGenerationTasks.value = await listAiGenerationTasks(
      currentUser.value.id,
      selectedCourse.value.id
    )
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
      replaceItem(aiProviders.value, saved)
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

function openTeacherProfileEditor() {
  if (!selectedTeacherProfile.value) return
  teacherEditForm.teacherName = selectedTeacherProfile.value.teacherName || ''
  teacherEditForm.confidenceScore = selectedTeacherProfile.value.confidenceScore ?? 50
  teacherEditForm.examStyle = selectedTeacherProfile.value.examStyle || ''
  teacherEditForm.questionPreference = selectedTeacherProfile.value.questionPreference || ''
  teacherEditForm.gradingPreference = selectedTeacherProfile.value.gradingPreference || ''
  teacherEditForm.focusTopics = selectedTeacherProfile.value.focusTopics || ''
  teacherEditForm.avoidTopics = selectedTeacherProfile.value.avoidTopics || ''
  teacherEditForm.sourceSummary = selectedTeacherProfile.value.sourceSummary || ''
  teacherProfileEditing.value = true
}

function cancelTeacherProfileEdit() {
  teacherProfileEditing.value = false
}

async function saveTeacherProfileEdit() {
  if (!selectedTeacherProfile.value || !currentUser.value) return
  if (!teacherEditForm.teacherName.trim()) {
    ElMessage.warning('请输入教师名称')
    return
  }
  teacherProfileSaving.value = true
  try {
    const profile = await updateTeacherProfile(selectedTeacherProfile.value.id, {
      userId: currentUser.value.id,
      teacherName: teacherEditForm.teacherName.trim(),
      confidenceScore: teacherEditForm.confidenceScore,
      examStyle: teacherEditForm.examStyle,
      questionPreference: teacherEditForm.questionPreference,
      gradingPreference: teacherEditForm.gradingPreference,
      focusTopics: teacherEditForm.focusTopics,
      avoidTopics: teacherEditForm.avoidTopics,
      sourceSummary: teacherEditForm.sourceSummary,
      analysisStatus: 'MANUAL_REVIEWED'
    })
    replaceItem(teacherProfiles.value, profile)
    selectedTeacherProfile.value = profile
    teacherProfileEditing.value = false
    ElMessage.success('教师画像已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    teacherProfileSaving.value = false
  }
}

async function runExport() {
  const payload = buildExportPayload()
  if (!payload) return
  exportCreating.value = true
  try {
    const record = await createExport(payload)
    exportRecords.value.unshift(record)
    exportPreview.value = null
    await loadCourseStats()
    ElMessage.success('知识包已生成')
    downloadExport(record)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exportCreating.value = false
  }
}

async function runExportPreview() {
  const payload = buildExportPayload()
  if (!payload) return
  exportPreviewLoading.value = true
  try {
    exportPreview.value = await previewExport(payload)
    ElMessage.success('导出预览已生成')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exportPreviewLoading.value = false
  }
}

function buildExportPayload() {
  if (!selectedCourse.value || !currentUser.value) return null
  if (!exportForm.exportName.trim()) {
    ElMessage.warning('请输入导出名称')
    return null
  }
  return {
    userId: currentUser.value.id,
    courseId: selectedCourse.value.id,
    templateId: exportForm.templateId,
    exportName: exportForm.exportName.trim(),
    exportFormat: 'ZIP',
    chapterIds: exportForm.chapterIds,
    materialTypes: exportForm.materialTypes,
    onlyKeyMaterials: exportForm.onlyKeyMaterials,
    includeExamStats: exportForm.includeExamStats,
    includePrerequisiteCourses: exportForm.includePrerequisiteCourses
      && hasCourseRelationsOfType('PREREQUISITE'),
    includeRelatedCourses: exportForm.includeRelatedCourses
      && hasCourseRelationsOfType('RELATED'),
    includeFollowUpCourses: exportForm.includeFollowUpCourses
      && hasCourseRelationsOfType('FOLLOW_UP')
  }
}

function downloadExport(record) {
  window.open(exportDownloadUrl(record.id, currentUser.value.id), '_blank')
}

function formatExportTime(value) {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString()
}

async function loadKnowledgeItems() {
  if (!selectedCourse.value) return
  knowledgeLoading.value = true
  try {
    knowledgeItems.value = await listKnowledgeItems(
      selectedCourse.value.id,
      currentUser.value.id,
      {
        itemType: knowledgeFilterType.value || undefined
      }
    )
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    knowledgeLoading.value = false
  }
}

async function loadCourseTags() {
  if (!selectedCourse.value) return
  try {
    courseTags.value = await listCourseTags(selectedCourse.value.id, currentUser.value.id)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadSelectedMaterialTags() {
  previewedMaterialTags.value = []
  if (!knowledgeForm.materialId) {
    selectedMaterialTags.value = []
    return
  }
  try {
    selectedMaterialTags.value = await listMaterialTags(
      knowledgeForm.materialId,
      currentUser.value.id
    )
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function saveSelectedMaterialTags() {
  if (!knowledgeForm.materialId) return
  tagSaving.value = true
  try {
    selectedMaterialTags.value = await replaceMaterialTags(
      knowledgeForm.materialId,
      currentUser.value.id,
      selectedMaterialTags.value
    )
    await loadCourseTags()
    ElMessage.success('标签已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    tagSaving.value = false
  }
}

async function previewTags() {
  if (!knowledgeForm.materialId) return
  tagPreviewLoading.value = true
  try {
    const result = await previewMaterialTags(
      knowledgeForm.materialId,
      currentUser.value.id,
      { model: 'deepseek-v4-flash' }
    )
    previewedMaterialTags.value = result.tags || []
    selectedMaterialTags.value = Array.from(new Set([
      ...selectedMaterialTags.value,
      ...previewedMaterialTags.value
    ]))
    await loadAiGenerationTasks()
    if (previewedMaterialTags.value.length === 0) {
      ElMessage.warning('AI 未提取到有效关键词')
    } else {
      ElMessage.success(`已提取 ${previewedMaterialTags.value.length} 个候选关键词`)
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    tagPreviewLoading.value = false
  }
}

function togglePreviewTag(tag) {
  if (selectedMaterialTags.value.includes(tag)) {
    selectedMaterialTags.value = selectedMaterialTags.value.filter(item => item !== tag)
  } else {
    selectedMaterialTags.value = [...selectedMaterialTags.value, tag]
  }
}

async function applyPreviewTags() {
  if (!knowledgeForm.materialId || previewedMaterialTags.value.length === 0) return
  await saveSelectedMaterialTags()
}

async function generateKnowledge() {
  if (!knowledgeForm.materialId) {
    ElMessage.warning('请选择已解析资料')
    return
  }
  knowledgeGenerating.value = true
  try {
    const result = await generateKnowledgeItems(
      knowledgeForm.materialId,
      currentUser.value.id,
      {
        maxItems: knowledgeForm.maxItems,
        replaceExisting: knowledgeForm.replaceExisting,
        model: 'deepseek-v4-flash'
      }
    )
    selectedMaterialTags.value = result.tags
    await Promise.all([loadKnowledgeItems(), loadCourseTags()])
    await loadCourseStats()
    ElMessage.success(`AI 已整理 ${result.items.length} 条知识`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    await loadAiGenerationTasks()
    knowledgeGenerating.value = false
  }
}

async function saveKnowledgeMastery(item) {
  if (!currentUser.value || !item?.id) return
  if (item.masteryScore !== null && item.masteryScore !== undefined) {
    const score = Number(item.masteryScore)
    if (Number.isNaN(score) || score < 0 || score > 100) {
      ElMessage.warning('掌握分数必须在 0 到 100 之间')
      return
    }
  }
  masterySavingIds.value = [...new Set([...masterySavingIds.value, item.id])]
  try {
    const status = await updateKnowledgeMastery(item.id, {
      userId: currentUser.value.id,
      masteryStatus: item.masteryStatus || 'UNKNOWN',
      masteryScore: item.masteryScore ?? null,
      note: item.masteryNote?.trim() || null
    })
    item.masteryStatus = status.masteryStatus
    item.masteryScore = status.masteryScore
    item.masteryNote = status.note
    item.lastReviewTime = status.lastReviewTime
    item.masteryUpdateTime = status.updateTime
    ElMessage.success('掌握状态已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    masterySavingIds.value = masterySavingIds.value.filter(id => id !== item.id)
  }
}

function isMasterySaving(itemId) {
  return masterySavingIds.value.includes(itemId)
}

function formatReviewTime(value) {
  return value ? new Date(value).toLocaleString() : '尚未复习'
}

function formatSearchTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function searchTypeLabel(type) {
  if (type === 'UNIFIED_KNOWLEDGE') return '统一检索'
  return type || '检索'
}

function knowledgeTypeLabel(type) {
  return knowledgeTypeOptions.find(option => option.type === type)?.label || type
}

async function runSearch() {
  if (!searchForm.keyword.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  searchLoading.value = true
  try {
    searchResults.value = await searchMaterials({
      userId: currentUser.value.id,
      courseId: selectedCourse.value.id,
      keyword: searchForm.keyword.trim(),
      chapterId: searchForm.chapterId || undefined,
      materialType: searchForm.materialType || undefined,
      isKey: searchForm.isKey
    })
    searchHasRun.value = true
    await loadSearchRecords()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    searchLoading.value = false
  }
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.chapterId = null
  searchForm.materialType = null
  searchForm.isKey = false
  searchResults.value = []
  searchHasRun.value = false
}

async function rerunSearchRecord(record) {
  if (!record?.keyword) return
  searchForm.keyword = record.keyword
  await runSearch()
}

function resetRelationForm() {
  relationForm.relatedCourseId = null
  relationForm.relationType = 'PREREQUISITE'
  relationForm.reason = ''
}

function resetGapForm() {
  gapForm.reportName = selectedCourse.value
    ? `${selectedCourse.value.courseName} 知识缺口报告`
    : ''
  gapForm.includePrerequisites = true
}

function clearGapState() {
  gapReports.value = []
  gapItems.value = []
  selectedGapReport.value = null
  resetGapForm()
}

function resetTeacherProfileForm() {
  teacherProfileForm.teacherName = ''
  teacherProfileForm.materialIds = []
}

function clearTeacherProfileState() {
  teacherProfiles.value = []
  teacherProfileEvidence.value = []
  selectedTeacherProfile.value = null
  resetTeacherProfileForm()
}

function resetReviewProfileForm() {
  editingReviewProfileId.value = null
  reviewProfileForm.profileName = selectedCourse.value
    ? `${selectedCourse.value.courseName} 复习配置`
    : ''
  reviewProfileForm.target = ''
  reviewProfileForm.difficultyLevel = 'MEDIUM'
  reviewProfileForm.outputType = 'REVIEW_NOTE'
  reviewProfileForm.includePrerequisites = true
  reviewProfileForm.teacherProfileId = null
  reviewProfileForm.customRequirement = ''
}

function clearReviewProfileState() {
  reviewProfiles.value = []
  resetReviewProfileForm()
}

function resetAiProviderForm() {
  editingAiProviderId.value = null
  aiProviderForm.providerName = 'DeepSeek'
  aiProviderForm.baseUrl = 'https://api.deepseek.com'
  aiProviderForm.modelName = 'deepseek-v4-flash'
  aiProviderForm.apiKeyAlias = 'DEEPSEEK_API_KEY'
  aiProviderForm.enabled = true
}

function clearAiProviderState() {
  aiProviders.value = []
  aiDefaultStatus.value = null
  aiGenerationTasks.value = []
  resetAiProviderForm()
}

function resetExportForm() {
  exportForm.exportName = selectedCourse.value
    ? `${selectedCourse.value.courseName} 知识包`
    : ''
  exportForm.templateId = exportTemplates.value[0]?.id || null
  exportForm.chapterIds = []
  exportForm.materialTypes = []
  exportForm.onlyKeyMaterials = false
  exportForm.includeExamStats = true
  exportForm.includePrerequisiteCourses = false
  exportForm.includeRelatedCourses = false
  exportForm.includeFollowUpCourses = false
  exportPreview.value = null
}

function hasCourseRelationsOfType(type) {
  return courseRelations.value.some(relation => relation.relationType === type)
}

function materialTypeLabel(type) {
  return materialTypeOptions.find(option => option.type === type)?.label || type
}

function masteryStatusLabel(status) {
  return masteryStatusOptions.find(option => option.status === status)?.label || '未评估'
}

function gapTypeLabel(type) {
  if (type === 'WEAK_MASTERY') return '掌握薄弱'
  if (type === 'NEED_REVIEW') return '需要复习'
  if (type === 'HIGH_FREQUENCY') return '高频考点'
  if (type === 'PREREQUISITE_GAP') return '前置缺口'
  return '未评估'
}

function relationTypeLabel(type) {
  return relationTypeOptions.find(option => option.type === type)?.label || '当前课程'
}

function formatGapTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function teacherStatusLabel(status) {
  if (status === 'RUNNING') return '分析中'
  if (status === 'SUCCESS') return '已完成'
  if (status === 'FAILED') return '失败'
  if (status === 'MANUAL_REVIEWED') return '已人工确认'
  return '等待分析'
}

function evidenceTypeLabel(type) {
  if (type === 'QUESTION_TYPE') return '题型证据'
  if (type === 'GRADING') return '评分证据'
  if (type === 'FOCUS_TOPIC') return '重点证据'
  if (type === 'AVOID_TOPIC') return '规避证据'
  return '风格证据'
}

function reviewDifficultyLabel(value) {
  return reviewDifficultyOptions.find(option => option.value === value)?.label || value
}

function reviewOutputLabel(value) {
  return reviewOutputOptions.find(option => option.value === value)?.label || value
}

function aiTaskTypeLabel(type) {
  if (type === 'TEACHER_PROFILE') return '教师画像分析'
  if (type === 'EXAM_MAPPING') return '真题知识映射'
  if (type === 'KNOWLEDGE_GAP') return '知识缺口检测'
  if (type === 'REVIEW_GENERATION') return '复习资料生成'
  if (type === 'MOCK_EXAM') return '模拟题生成'
  if (type === 'PACKAGE_SUMMARY') return '知识包摘要'
  if (type === 'KNOWLEDGE_EXTRACTION') return '资料知识整理'
  if (type === 'MATERIAL_SUMMARY') return '资料摘要生成'
  if (type === 'MATERIAL_TAG_EXTRACTION') return '资料关键词提取'
  return type || '未知任务'
}

function aiTaskStatusLabel(status) {
  if (status === 'RUNNING') return '运行中'
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'CANCELED') return '已取消'
  return '等待中'
}

function formatAiTaskTime(value) {
  return value ? new Date(value).toLocaleString() : '刚刚'
}

function matchSourceLabel(source) {
  if (source === 'TITLE') return '标题命中'
  if (source === 'SUMMARY') return '摘要命中'
  if (source === 'TAG') return '标签命中'
  if (source === 'KNOWLEDGE_TITLE') return '知识标题命中'
  if (source === 'KNOWLEDGE_CONTENT') return '知识内容命中'
  return '正文命中'
}

function searchResultMaterial(result) {
  return materials.value.find(material => material.id === result.materialId) || {
    id: result.materialId,
    courseId: result.courseId,
    chapterId: result.chapterId,
    title: result.title,
    materialType: result.materialType,
    summary: result.summary,
    year: result.year,
    key: result.key,
    originalName: result.originalName,
    fileType: result.fileType,
    fileSize: result.fileSize,
    parsedChunkCount: result.parsedChunkCount
  }
}

function editSearchResult(result) {
  openMaterialEditor(searchResultMaterial(result))
}

function previewSearchResult(result) {
  showParsedText(searchResultMaterial(result))
}

function openKnowledgeResult() {
  activeSection.value = 'knowledge'
}

async function saveMaterial() {
  if (!editingMaterialId.value && uploadDrafts.value.length === 0) {
    ElMessage.warning('请选择资料文件')
    return
  }
  if (editingMaterialId.value && !materialForm.title.trim()) {
    ElMessage.warning('请输入资料标题')
    return
  }
  if (!editingMaterialId.value) {
    const missingTitle = uploadDrafts.value.find(draft => !draft.title.trim())
    if (missingTitle) {
      ElMessage.warning(`请填写「${missingTitle.name}」的资料标题`)
      return
    }
    const missingFile = uploadDrafts.value.find(draft => !draft.file)
    if (missingFile) {
      ElMessage.warning(`请重新选择「${missingFile.name}」`)
      return
    }
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
      await loadCourseStats()
      materialDialogVisible.value = false
      ElMessage.success('资料信息已更新')
      return
    }

    const data = new FormData()
    data.append('userId', currentUser.value.id)
    data.append('courseId', selectedCourse.value.id)
    uploadDrafts.value.forEach(draft => {
      data.append('files', draft.file)
      data.append('titles', draft.title.trim())
      data.append('materialTypes', draft.materialType)
      data.append('chapterIds', draft.chapterId || '')
      data.append('years', draft.year || '')
      data.append('isKeys', draft.isKey)
      data.append('summaries', draft.summary.trim())
    })

    const uploadedMaterials = await uploadMaterialsBatch(data)
    materials.value.unshift(...uploadedMaterials)
    await loadCourseStats()
    await checkSimilarMaterials(uploadedMaterials)
    resetMaterialForm()
    materialDialogVisible.value = false
    ElMessage.success(`已上传 ${uploadedMaterials.length} 份资料`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    materialSaving.value = false
  }
}

function handleFileChange(uploadFile, uploadFiles) {
  syncUploadDrafts(uploadFiles)
}

function handleFileRemove(uploadFile, uploadFiles) {
  syncUploadDrafts(uploadFiles)
}

function syncUploadDrafts(uploadFiles = []) {
  uploadFileList.value = uploadFiles
  const existingDrafts = new Map(uploadDrafts.value.map(draft => [draft.uid, draft]))
  uploadDrafts.value = uploadFiles.map(uploadFile => {
    const existing = existingDrafts.get(uploadFile.uid)
    if (existing) {
      existing.file = uploadFile.raw
      existing.name = uploadFile.name
      existing.size = uploadFile.size || uploadFile.raw?.size || 0
      return existing
    }
    return createUploadDraft(uploadFile)
  })
}

function createUploadDraft(uploadFile) {
  const name = uploadFile.name || uploadFile.raw?.name || ''
  return {
    uid: uploadFile.uid,
    file: uploadFile.raw,
    name,
    size: uploadFile.size || uploadFile.raw?.size || 0,
    title: name.replace(/\.[^.]+$/, ''),
    materialType: materialForm.materialType,
    chapterId: materialForm.chapterId,
    year: materialForm.year,
    isKey: materialForm.isKey,
    summary: materialForm.summary
  }
}

async function runPdfParse(material) {
  parsingMaterialId.value = material.id
  try {
    const result = await parsePdf(material.id, currentUser.value.id)
    material.parsedChunkCount = result.chunkCount
    await loadCourseStats()
    await checkSimilarMaterials([material])
    ElMessage.success(`解析完成，共提取 ${result.chunkCount} 个文本块`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    parsingMaterialId.value = null
  }
}

function isSummaryGenerating(materialId) {
  return summaryGeneratingIds.value.includes(materialId)
}

async function runMaterialSummary(material) {
  if (!material.parsedChunkCount) {
    ElMessage.warning('请先解析资料文本')
    return
  }
  summaryGeneratingIds.value.push(material.id)
  try {
    const updated = await generateMaterialSummary(
      material.id,
      currentUser.value.id,
      { model: 'deepseek-v4-flash' }
    )
    replaceItem(materials.value, updated)
    await loadAiGenerationTasks()
    ElMessage.success('资料摘要已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    summaryGeneratingIds.value = summaryGeneratingIds.value.filter(id => id !== material.id)
  }
}

async function checkSimilarMaterials(sourceMaterials) {
  for (const material of sourceMaterials) {
    try {
      const results = await listSimilarMaterials(material.id, currentUser.value.id)
      if (results.length > 0) {
        similarSourceMaterial.value = material
        similarMaterialResults.value = results
        similarDialogVisible.value = true
        return
      }
    } catch (error) {
      ElMessage.warning(`相似资料检测失败：${error.message}`)
    }
  }
}

function openSimilarMaterial(material) {
  similarDialogVisible.value = false
  openMaterialEditor(material)
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

function openExamWorkbench(material) {
  examPreferredMaterialId.value = material.id
  scrollToSection('exam')
}

function handleExamMaterialParsed({ materialId, chunkCount }) {
  const target = materials.value.find(item => item.id === materialId)
  if (target) {
    target.parsedChunkCount = chunkCount
  }
  loadCourseStats()
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

function confirmCourseDeletion() {
  deleteTarget.value = {
    type: 'course',
    id: selectedCourse.value.id,
    name: selectedCourse.value.courseName
  }
  deleteDialogVisible.value = true
}

function confirmChapterDeletion(chapter = null) {
  const target = chapter || chapters.value.find(item => item.id === editingChapterId.value)
  if (!target) return
  deleteTarget.value = {
    type: 'chapter',
    id: target.id,
    name: `${target.chapterNo} ${target.chapterTitle}`
  }
  deleteDialogVisible.value = true
}

function confirmMaterialDeletion(material = null) {
  const target = material || materials.value.find(item => item.id === editingMaterialId.value)
  if (!target) return
  deleteTarget.value = {
    type: 'material',
    id: target.id,
    name: target.title
  }
  deleteDialogVisible.value = true
}

function confirmKnowledgeDeletion(item) {
  deleteTarget.value = {
    type: 'knowledge',
    id: item.id,
    name: item.title
  }
  deleteDialogVisible.value = true
}

async function executeDeletion() {
  if (!deleteTarget.value) return
  deleteLoading.value = true
  try {
    if (deleteTarget.value.type === 'course') {
      await removeCourse(deleteTarget.value.id)
    } else if (deleteTarget.value.type === 'chapter') {
      await removeChapter(deleteTarget.value.id)
    } else if (deleteTarget.value.type === 'knowledge') {
      await removeKnowledgeItem(deleteTarget.value.id)
    } else {
      await removeMaterial(deleteTarget.value.id)
    }
  } finally {
    deleteLoading.value = false
  }
}

async function removeKnowledgeItem(itemId) {
  try {
    await deleteKnowledgeItem(
      selectedCourse.value.id,
      itemId,
      currentUser.value.id
    )
    knowledgeItems.value = knowledgeItems.value.filter(item => item.id !== itemId)
    await loadCourseStats()
    deleteDialogVisible.value = false
    ElMessage.success('知识条目已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeCourse(courseId) {
  try {
    await deleteCourse(courseId, currentUser.value.id)
    courses.value = courses.value.filter(course => course.id !== courseId)
    courseDialogVisible.value = false
    deleteDialogVisible.value = false
    if (courses.value.length > 0) {
      await selectCourse(courses.value[0])
    } else {
      selectedCourse.value = null
      chapters.value = []
      materials.value = []
      courseRelations.value = []
      clearGapState()
      clearTeacherProfileState()
      clearReviewProfileState()
      clearAiProviderState()
    }
    ElMessage.success('课程已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeChapter(chapterId) {
  try {
    await deleteChapter(selectedCourse.value.id, chapterId, currentUser.value.id)
    chapters.value = chapters.value.filter(item => item.id !== chapterId)
    materials.value.forEach(material => {
      if (material.chapterId === chapterId) {
        material.chapterId = null
      }
    })
    chapterDialogVisible.value = false
    deleteDialogVisible.value = false
    ElMessage.success('章节已删除，相关资料已保留')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeMaterial(materialId) {
  try {
    await deleteMaterial(materialId, currentUser.value.id)
    materials.value = materials.value.filter(item => item.id !== materialId)
    await loadCourseStats()
    materialDialogVisible.value = false
    deleteDialogVisible.value = false
    ElMessage.success('资料已删除')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function resetDeleteTarget() {
  deleteTarget.value = null
}

function logout() {
  localStorage.removeItem('ai4note-user')
  currentUser.value = null
  courses.value = []
  chapters.value = []
  materials.value = []
  courseRelations.value = []
  courseStats.value = null
  searchRecords.value = []
  selectedCourse.value = null
  clearGapState()
  clearTeacherProfileState()
  clearReviewProfileState()
  clearAiProviderState()
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
  materialUploadRef.value?.clearFiles()
  editingMaterialId.value = null
  materialForm.title = ''
  materialForm.materialType = 'SLIDE'
  materialForm.chapterId = null
  materialForm.year = new Date().getFullYear()
  materialForm.isKey = false
  materialForm.summary = ''
  uploadFileList.value = []
  uploadDrafts.value = []
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
  padding: 15px 18px;
  border: 1px solid #111;
  background: rgba(255, 255, 255, 0.9);
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  font-size: 34px;
  line-height: 1;
}

.hero-stats span {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.course-type-distribution {
  position: relative;
  z-index: 2;
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(180px, 0.22fr) minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
  margin-top: 8px;
  padding: 18px;
  border: 1px solid #111;
  background: rgba(255, 255, 255, 0.92);
  opacity: 0;
  transform: translateY(36px);
  transition: opacity 0.7s 0.28s ease, transform 0.7s 0.28s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.type-distribution-heading {
  display: grid;
  align-content: center;
  border-right: 1px solid #111;
}

.type-distribution-heading strong,
.type-distribution-heading span {
  display: block;
}

.type-distribution-heading strong {
  font-size: 28px;
  letter-spacing: -0.04em;
}

.type-distribution-heading span {
  margin-top: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.type-distribution-list {
  display: grid;
  gap: 10px;
}

.type-distribution-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 42px;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  font-weight: 850;
}

.type-distribution-row > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-distribution-row > div {
  height: 9px;
  border: 1px solid #111;
  background: #fff;
}

.type-distribution-row i {
  display: block;
  height: 100%;
  min-width: 0;
  background: #14cbea;
}

.type-distribution-row strong {
  text-align: right;
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
.course-hero.section-active .course-type-distribution,
.course-hero.section-active .hero-orbit-one,
.course-hero.section-active .hero-orbit-two {
  opacity: 1;
  transform: translate(0) rotate(0) scale(1);
}

.relation-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 96px;
  background: #f5f3ef;
}

.relation-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.relation-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.relation-heading h2 span {
  color: #ff3151;
}

.relation-heading > p {
  margin: 0 0 5px;
  color: #3f4352;
  font-size: 15px;
  line-height: 1.75;
}

.relation-panel {
  margin-top: 48px;
  display: grid;
  gap: 26px;
}

.relation-form {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px minmax(260px, 1.2fr) 140px;
  gap: 12px;
  align-items: end;
  padding: 24px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #ff3151;
}

.relation-form label {
  min-width: 0;
}

.relation-form label > span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

:deep(.relation-form .el-select),
:deep(.relation-form .el-input) {
  width: 100%;
}

:deep(.relation-form .el-select__wrapper),
:deep(.relation-form .el-input__wrapper) {
  min-height: 50px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.relation-form button {
  min-height: 50px;
  border: 1px solid #111;
  background: #ffef5a;
  font-weight: 900;
  cursor: pointer;
}

.relation-form button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.relation-groups {
  min-height: 260px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.relation-group {
  min-width: 0;
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
}

.relation-group-heading {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #111;
}

.relation-group-heading strong {
  font-size: 22px;
  letter-spacing: -0.04em;
}

.relation-group-heading span {
  color: #666;
  font-size: 12px;
  font-weight: 850;
}

.relation-card {
  display: grid;
  gap: 14px;
  margin-top: 14px;
  padding: 16px;
  border: 1px solid #111;
  box-shadow: 6px 6px 0 #14cbea;
}

.relation-card strong {
  display: block;
  overflow: hidden;
  font-size: 20px;
  letter-spacing: -0.035em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-card p,
.relation-card small {
  display: block;
  margin: 8px 0 0;
  color: #666;
  line-height: 1.55;
}

.relation-card button {
  justify-self: end;
  padding: 0;
  border: 0;
  background: transparent;
  color: #ff3151;
  font-size: 11px;
  font-weight: 850;
  cursor: pointer;
}

.relation-empty {
  min-height: 110px;
  display: grid;
  place-content: center;
  color: #777;
  font-size: 13px;
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

.chapter-card-actions {
  align-self: flex-end;
  display: flex;
  gap: 16px;
}

.chapter-card-actions button {
  align-self: auto;
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

.danger-text {
  color: #ff3151 !important;
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

.search-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  background: #e9fbf7;
}

.search-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.search-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.search-heading h2 span {
  color: #ff3151;
}

.search-heading > p {
  margin: 0 0 5px;
  font-size: 15px;
  line-height: 1.75;
}

.search-panel {
  margin-top: 48px;
  padding: 24px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #14cbea;
}

.search-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px;
  gap: 12px;
}

:deep(.search-input-row .el-input__wrapper),
:deep(.search-filters .el-select__wrapper) {
  min-height: 52px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

:deep(.search-input-row .el-input__inner) {
  font-size: 16px;
  font-weight: 700;
}

.search-input-row > button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border: 1px solid #111;
  border-radius: 999px;
  background: #111;
  color: #fff;
  font-weight: 850;
  cursor: pointer;
}

.search-input-row > button strong {
  font-size: 20px;
}

.search-input-row > button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.search-filters {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.search-key-filter {
  min-height: 52px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid #111;
  font-size: 12px;
  font-weight: 800;
}

.search-reset {
  padding: 0 12px;
  border: 0;
  background: transparent;
  font-weight: 800;
  cursor: pointer;
}

.search-reset:hover {
  color: #ff3151;
}

.search-history {
  margin-top: 28px;
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 8px 8px 0 #111;
}

.search-history-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #111;
}

.search-history-heading strong,
.search-history-heading span {
  display: block;
}

.search-history-heading strong {
  font-size: 34px;
  letter-spacing: -0.05em;
}

.search-history-heading span {
  color: #666;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.search-history-heading button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #111;
  background: #14cbea;
  font-weight: 850;
  cursor: pointer;
}

.search-history-list {
  max-height: 240px;
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.search-history-list button {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 7px 10px;
  align-items: center;
  padding: 14px;
  border: 1px solid #111;
  background: #f9faf6;
  color: #111;
  text-align: left;
  cursor: pointer;
}

.search-history-list button:hover {
  background: #ffef5a;
}

.search-history-list strong {
  min-width: 0;
  overflow: hidden;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-history-list span,
.search-history-list small {
  color: #666;
  font-size: 11px;
  font-weight: 800;
}

.search-history-list small {
  grid-column: 1 / -1;
}

.search-history-empty {
  min-height: 130px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.search-history-empty strong {
  color: #111;
  font-size: 20px;
}

.search-history-empty p {
  margin: 8px 0 0;
  font-size: 12px;
}

.search-result-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  margin: 54px 0 18px;
  padding-bottom: 13px;
  border-bottom: 1px solid #111;
}

.search-result-heading strong {
  font-size: 24px;
  letter-spacing: -0.04em;
}

.search-result-heading span {
  color: #666;
  font-size: 12px;
}

.search-results {
  min-height: 240px;
  display: grid;
  gap: 12px;
}

.search-result-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 22px;
  align-items: start;
  padding: 24px;
  border: 1px solid #111;
  background: #fff;
  transition: transform 0.2s, box-shadow 0.2s;
}

.search-result-card:hover {
  transform: translate(-3px, -3px);
  box-shadow: 7px 7px 0 #14cbea;
}

.search-result-card.search-result-knowledge {
  box-shadow: inset 5px 0 0 #ad93ff;
}

.search-result-card.search-result-knowledge:hover {
  box-shadow: 7px 7px 0 #ad93ff, inset 5px 0 0 #ad93ff;
}

.search-result-index {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border: 1px solid #111;
  border-radius: 50%;
  background: #14cbea;
  font-size: 11px;
  font-weight: 900;
}

.search-result-content {
  min-width: 0;
}

.search-result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.search-result-meta span {
  padding: 4px 8px;
  border: 1px solid #111;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 850;
}

.search-result-content h3 {
  margin: 15px 0 9px;
  font-size: 25px;
  letter-spacing: -0.045em;
}

.search-result-content p {
  margin: 0;
  color: #444;
  font-size: 14px;
  line-height: 1.75;
}

.search-result-content small {
  display: block;
  margin-top: 13px;
  overflow: hidden;
  color: #777;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-actions {
  min-width: 120px;
  display: grid;
  justify-items: end;
  gap: 12px;
}

.search-result-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 11px;
  font-weight: 850;
  cursor: pointer;
}

.search-result-actions button:hover {
  color: #ff3151;
  text-decoration: underline;
  text-underline-offset: 4px;
}

.search-empty {
  min-height: 220px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.search-empty strong {
  color: #111;
  font-size: 20px;
}

.search-empty p {
  margin: 10px 0 0;
  font-size: 12px;
}

.knowledge-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  color: #fff;
  background: #111;
}

.knowledge-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.knowledge-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.knowledge-heading h2 span {
  color: #ad93ff;
}

.knowledge-heading > p {
  margin: 0 0 5px;
  color: #ccc;
  font-size: 15px;
  line-height: 1.75;
}

.knowledge-generator {
  margin-top: 48px;
  padding: 24px;
  border: 1px solid #fff;
  background: #191919;
  box-shadow: 10px 10px 0 #ad93ff;
}

.knowledge-generator-main {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 130px auto 190px;
  gap: 12px;
  align-items: center;
}

:deep(.knowledge-generator .el-select__wrapper),
:deep(.knowledge-generator .el-input__wrapper),
:deep(.knowledge-toolbar .el-select__wrapper),
:deep(.knowledge-tags-panel .el-select__wrapper) {
  min-height: 50px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.knowledge-replace {
  min-height: 50px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 14px;
  border: 1px solid #fff;
  font-size: 11px;
  font-weight: 800;
}

.knowledge-generate-button {
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border: 1px solid #fff;
  border-radius: 999px;
  background: #ad93ff;
  color: #111;
  font-weight: 900;
  cursor: pointer;
}

.knowledge-generate-button strong {
  font-size: 20px;
}

.knowledge-generate-button:disabled {
  opacity: 0.5;
  cursor: wait;
}

.knowledge-generator > p {
  margin: 8px 0 0;
  padding-left: calc(100% - 190px - 12px - 130px - 12px - 210px);
  color: #888;
  font-size: 10px;
}

.knowledge-tags-panel {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr) 110px 150px;
  gap: 14px;
  align-items: center;
  margin-top: 24px;
  padding: 20px;
  border: 1px solid #555;
  background: #171717;
}

.knowledge-tags-panel strong,
.knowledge-tags-panel span {
  display: block;
}

.knowledge-tags-panel span {
  margin-top: 5px;
  color: #888;
  font-size: 10px;
}

.knowledge-tags-panel button {
  min-height: 48px;
  border: 1px solid #fff;
  background: #fff;
  color: #111;
  font-weight: 850;
  cursor: pointer;
}

.knowledge-tags-panel button:disabled {
  opacity: 0.5;
  cursor: wait;
}

.tag-preview-panel {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid #444;
}

.tag-preview-panel > div {
  min-width: 160px;
  margin-right: 6px;
}

.tag-preview-panel button {
  min-height: 34px;
  padding: 0 12px;
  border-color: #ad93ff;
  background: transparent;
  color: #fff;
  font-size: 12px;
}

.tag-preview-panel button.active {
  background: #ad93ff;
  color: #111;
}

.tag-preview-panel .tag-preview-apply {
  margin-left: auto;
  border-color: #fff;
  background: #fff;
  color: #111;
}

.knowledge-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-top: 52px;
  padding-bottom: 16px;
  border-bottom: 1px solid #555;
}

.knowledge-toolbar > div strong,
.knowledge-toolbar > div span {
  display: block;
}

.knowledge-toolbar > div strong {
  font-size: 40px;
  letter-spacing: -0.06em;
}

.knowledge-toolbar > div span {
  color: #888;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.knowledge-toolbar .el-select {
  width: 190px;
}

.knowledge-grid {
  min-height: 260px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.knowledge-card {
  --knowledge-accent: #ad93ff;
  position: relative;
  padding: 24px;
  border: 1px solid #555;
  background: #181818;
  box-shadow: inset 0 5px 0 var(--knowledge-accent);
}

.knowledge-card-definition { --knowledge-accent: #14cbea; }
.knowledge-card-key_point { --knowledge-accent: #ffb21c; }
.knowledge-card-formula { --knowledge-accent: #ad93ff; }
.knowledge-card-method { --knowledge-accent: #0de0c0; }
.knowledge-card-example { --knowledge-accent: #ffc0d0; }
.knowledge-card-warning { --knowledge-accent: #ff3151; }

.knowledge-card-top,
.knowledge-source {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-card-top {
  justify-content: space-between;
}

.knowledge-card-top > span {
  padding: 5px 9px;
  border: 1px solid #fff;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 900;
}

.knowledge-stars {
  color: var(--knowledge-accent);
  font-size: 8px;
  letter-spacing: 4px;
}

.knowledge-card h3 {
  margin: 21px 0 12px;
  font-size: 27px;
  letter-spacing: -0.045em;
}

.knowledge-card > p {
  margin: 0;
  color: #d2d2d2;
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.knowledge-source {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px solid #444;
  color: #999;
  font-size: 10px;
}

.knowledge-mastery {
  display: grid;
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #444;
  background: #111;
}

.knowledge-mastery-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
}

.knowledge-mastery label {
  min-width: 0;
}

.knowledge-mastery label > span {
  display: block;
  margin-bottom: 7px;
  color: #aaa;
  font-size: 10px;
  font-weight: 850;
}

:deep(.knowledge-mastery .el-select),
:deep(.knowledge-mastery .el-input-number) {
  width: 100%;
}

:deep(.knowledge-mastery .el-select__wrapper),
:deep(.knowledge-mastery .el-input__wrapper),
:deep(.knowledge-mastery .el-textarea__inner) {
  border: 1px solid #555;
  border-radius: 0;
  box-shadow: none;
}

.knowledge-mastery-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.knowledge-mastery-actions span {
  color: #999;
  font-size: 10px;
}

.knowledge-mastery-actions button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #fff;
  background: #fff;
  color: #111;
  font-size: 11px;
  font-weight: 850;
  cursor: pointer;
}

.knowledge-mastery-actions button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.knowledge-card > button {
  display: block;
  margin: 16px 0 0 auto;
  padding: 0;
  border: 0;
  background: transparent;
  font-size: 10px;
  font-weight: 850;
  cursor: pointer;
}

.knowledge-empty {
  grid-column: 1 / -1;
  min-height: 240px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #666;
  color: #888;
  text-align: center;
}

.knowledge-empty strong {
  color: #fff;
  font-size: 20px;
}

.knowledge-empty p {
  margin: 10px 0 0;
  font-size: 12px;
}

.gap-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  color: #111;
  background: #f5f3ef;
}

.gap-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.gap-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.gap-heading h2 span {
  color: #ff3151;
}

.gap-heading > p {
  margin: 0 0 5px;
  color: #3f4352;
  font-size: 15px;
  line-height: 1.75;
}

.gap-generator {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 190px 150px;
  gap: 12px;
  align-items: end;
  margin-top: 48px;
  padding: 24px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #ff3151;
}

.gap-generator label > span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

:deep(.gap-generator .el-input__wrapper) {
  min-height: 50px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.gap-switch {
  min-height: 50px;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  border: 1px solid #111;
  font-size: 12px;
  font-weight: 850;
}

.gap-switch small {
  color: #666;
}

.gap-generator button {
  min-height: 50px;
  border: 1px solid #111;
  background: #ffef5a;
  font-weight: 900;
  cursor: pointer;
}

.gap-generator button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.gap-layout {
  display: grid;
  grid-template-columns: 290px minmax(0, 1fr);
  gap: 22px;
  margin-top: 50px;
}

.gap-report-list {
  min-height: 360px;
  padding: 18px;
  border: 1px solid #111;
  background: #fff;
}

.gap-report-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #111;
}

.gap-report-heading strong {
  font-size: 34px;
  letter-spacing: -0.06em;
}

.gap-report-heading span {
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.gap-report-list > button {
  width: 100%;
  display: grid;
  gap: 7px;
  margin-top: 12px;
  padding: 14px;
  border: 1px solid #111;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.gap-report-list > button.active {
  background: #111;
  color: #fff;
}

.gap-report-list > button strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gap-report-list > button span {
  color: #777;
  font-size: 11px;
}

.gap-report-list > button.active span {
  color: #ccc;
}

.gap-empty-list {
  min-height: 170px;
  display: grid;
  place-content: center;
  color: #777;
  font-size: 13px;
}

.gap-detail {
  min-height: 360px;
}

.gap-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.gap-summary > div {
  padding: 18px;
  border: 1px solid #111;
  background: #fff;
}

.gap-summary span,
.gap-summary strong {
  display: block;
}

.gap-summary span {
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.gap-summary strong {
  margin-top: 8px;
  font-size: 38px;
  letter-spacing: -0.06em;
}

.gap-report-summary {
  margin: 16px 0 0;
  padding: 16px;
  border: 1px solid #111;
  background: #fff;
  color: #333;
  line-height: 1.7;
}

.gap-item-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.gap-item-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 7px 7px 0 #111;
}

.gap-item-top,
.gap-item-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.gap-item-top {
  justify-content: space-between;
}

.gap-item-top span,
.gap-item-meta span {
  padding: 4px 8px;
  border: 1px solid #111;
  font-size: 10px;
  font-weight: 850;
}

.gap-item-top strong {
  color: #ff3151;
  font-size: 12px;
}

.gap-item-card h3 {
  margin: 16px 0 12px;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.gap-item-card p {
  margin: 14px 0 0;
  color: #333;
  line-height: 1.7;
}

.gap-item-card small {
  display: block;
  margin-top: 12px;
  color: #666;
  line-height: 1.6;
}

.gap-empty-detail {
  min-height: 360px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.gap-empty-detail strong {
  color: #111;
  font-size: 20px;
}

.gap-empty-detail p {
  margin: 10px 0 0;
  font-size: 12px;
}

.teacher-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  color: #fff;
  background: #111;
}

.teacher-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.teacher-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.teacher-heading h2 span {
  color: #0de0c0;
}

.teacher-heading > p {
  margin: 0 0 5px;
  color: #ccc;
  font-size: 15px;
  line-height: 1.75;
}

.teacher-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 24px;
  margin-top: 48px;
}

.teacher-control {
  display: grid;
  gap: 18px;
  align-content: start;
}

.teacher-form,
.teacher-profile-list,
.teacher-profile-detail {
  border: 1px solid #555;
  background: #181818;
}

.teacher-form {
  display: grid;
  gap: 14px;
  padding: 20px;
  box-shadow: 8px 8px 0 #0de0c0;
}

.teacher-form label > span {
  display: block;
  margin-bottom: 8px;
  color: #aaa;
  font-size: 11px;
  font-weight: 850;
}

:deep(.teacher-form .el-select),
:deep(.teacher-form .el-input) {
  width: 100%;
}

:deep(.teacher-form .el-select__wrapper),
:deep(.teacher-form .el-input__wrapper) {
  min-height: 50px;
  border: 1px solid #555;
  border-radius: 0;
  box-shadow: none;
}

.teacher-form button {
  min-height: 48px;
  border: 1px solid #fff;
  background: #0de0c0;
  color: #111;
  font-weight: 900;
  cursor: pointer;
}

.teacher-form button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.teacher-profile-list {
  min-height: 230px;
  padding: 16px;
}

.teacher-profile-list > button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 14px;
  border: 1px solid #555;
  background: transparent;
  color: #fff;
  text-align: left;
  cursor: pointer;
}

.teacher-profile-list > button.active {
  border-color: #0de0c0;
  box-shadow: inset 5px 0 0 #0de0c0;
}

.teacher-profile-list > button strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.teacher-profile-list > button span {
  color: #aaa;
  font-size: 11px;
}

.teacher-empty-list {
  min-height: 170px;
  display: grid;
  place-content: center;
  color: #888;
  font-size: 13px;
}

.teacher-profile-detail {
  min-height: 520px;
  padding: 24px;
}

.teacher-profile-top {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #555;
}

.teacher-profile-top span {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid #0de0c0;
  color: #0de0c0;
  font-size: 11px;
  font-weight: 850;
}

.teacher-profile-top h3 {
  margin: 14px 0 0;
  font-size: 42px;
  letter-spacing: -0.06em;
}

.teacher-profile-top strong {
  color: #0de0c0;
  font-size: 46px;
  letter-spacing: -0.06em;
}

.teacher-profile-score {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.teacher-profile-score button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #fff;
  background: transparent;
  color: #fff;
  font-size: 11px;
  font-weight: 850;
  cursor: pointer;
}

.teacher-edit-panel {
  margin-top: 22px;
  padding: 20px;
  border: 1px solid #0de0c0;
  background: #101010;
}

.teacher-edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.teacher-edit-grid label {
  min-width: 0;
}

.teacher-edit-grid label > span {
  display: block;
  margin-bottom: 8px;
  color: #aaa;
  font-size: 11px;
  font-weight: 850;
}

:deep(.teacher-edit-grid .el-input),
:deep(.teacher-edit-grid .el-input-number) {
  width: 100%;
}

:deep(.teacher-edit-grid .el-input__wrapper),
:deep(.teacher-edit-grid .el-textarea__inner) {
  border: 1px solid #555;
  border-radius: 0;
  box-shadow: none;
}

.teacher-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.teacher-edit-actions button {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #fff;
  background: transparent;
  color: #fff;
  font-weight: 850;
  cursor: pointer;
}

.teacher-edit-actions button:last-child {
  background: #0de0c0;
  color: #111;
}

.teacher-edit-actions button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.teacher-profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.teacher-profile-grid section {
  min-width: 0;
  padding: 18px;
  border: 1px solid #444;
  background: #141414;
}

.teacher-profile-grid span {
  display: block;
  color: #0de0c0;
  font-size: 11px;
  font-weight: 850;
}

.teacher-profile-grid p {
  margin: 12px 0 0;
  color: #ddd;
  line-height: 1.75;
  white-space: pre-wrap;
}

.teacher-evidence-panel {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #555;
}

.teacher-evidence-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
}

.teacher-evidence-heading strong {
  color: #0de0c0;
  font-size: 34px;
  letter-spacing: -0.06em;
}

.teacher-evidence-heading span {
  color: #aaa;
  font-size: 11px;
  font-weight: 850;
}

.teacher-evidence-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.teacher-evidence-list article {
  padding: 16px;
  border: 1px solid #444;
  background: #141414;
}

.teacher-evidence-list article > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.teacher-evidence-list strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.teacher-evidence-list span {
  flex: 0 0 auto;
  color: #0de0c0;
  font-size: 10px;
  font-weight: 850;
}

.teacher-evidence-list p {
  margin: 10px 0 0;
  color: #ddd;
  line-height: 1.65;
}

.teacher-evidence-list small {
  display: block;
  margin-top: 10px;
  color: #999;
}

.teacher-evidence-empty {
  min-height: 120px;
  display: grid;
  place-content: center;
  margin-top: 14px;
  border: 1px dashed #555;
  color: #888;
  font-size: 13px;
}

.teacher-empty-detail {
  min-height: 470px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #666;
  color: #888;
  text-align: center;
}

.teacher-empty-detail strong {
  color: #fff;
  font-size: 20px;
}

.teacher-empty-detail p {
  margin: 10px 0 0;
  font-size: 12px;
}

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
  grid-template-columns: minmax(320px, 0.42fr) minmax(0, 1fr);
  gap: 24px;
  margin-top: 48px;
}

.review-form {
  display: grid;
  gap: 14px;
  align-content: start;
  padding: 22px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #ffb21c;
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
  min-height: 420px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
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

.ai-config-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  color: #fff;
  background: #111;
}

.ai-config-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.ai-config-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.ai-config-heading h2 span {
  color: #ad93ff;
}

.ai-config-heading > p {
  margin: 0 0 5px;
  color: #ccc;
  font-size: 15px;
  line-height: 1.75;
}

.ai-config-status {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 44px;
}

.ai-config-status > div {
  padding: 18px;
  border: 1px solid #555;
  background: #181818;
}

.ai-config-status span,
.ai-config-status strong {
  display: block;
}

.ai-config-status span {
  color: #aaa;
  font-size: 11px;
  font-weight: 850;
}

.ai-config-status strong {
  margin-top: 8px;
  overflow: hidden;
  color: #ad93ff;
  font-size: 30px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-config-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.4fr) minmax(0, 1fr);
  gap: 24px;
  margin-top: 34px;
}

.ai-provider-form {
  display: grid;
  gap: 14px;
  align-content: start;
  padding: 22px;
  border: 1px solid #555;
  background: #181818;
  box-shadow: 8px 8px 0 #ad93ff;
}

.ai-provider-form label > span {
  display: block;
  margin-bottom: 8px;
  color: #aaa;
  font-size: 11px;
  font-weight: 850;
}

:deep(.ai-provider-form .el-input) {
  width: 100%;
}

:deep(.ai-provider-form .el-input__wrapper) {
  min-height: 48px;
  border: 1px solid #555;
  border-radius: 0;
  box-shadow: none;
}

.ai-provider-switch {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border: 1px solid #555;
  font-weight: 850;
}

.ai-provider-form > p {
  margin: 0;
  color: #999;
  font-size: 11px;
  line-height: 1.6;
}

.ai-provider-actions,
.ai-provider-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ai-provider-actions button,
.ai-provider-card-actions button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #fff;
  background: transparent;
  color: #fff;
  font-weight: 850;
  cursor: pointer;
}

.ai-provider-actions button:last-child {
  background: #ad93ff;
  color: #111;
}

.ai-provider-actions button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.ai-provider-list {
  min-height: 380px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
}

.ai-provider-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #555;
  background: #181818;
}

.ai-provider-card-top,
.ai-provider-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-provider-card-top {
  justify-content: space-between;
}

.ai-provider-card-top span,
.ai-provider-meta span {
  padding: 4px 8px;
  border: 1px solid #ad93ff;
  color: #ad93ff;
  font-size: 10px;
  font-weight: 850;
}

.ai-provider-card h3 {
  margin: 16px 0 8px;
  overflow: hidden;
  font-size: 25px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-provider-card p {
  margin: 0 0 14px;
  overflow: hidden;
  color: #ccc;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-provider-card-actions {
  margin-top: 16px;
}

.ai-provider-card-actions button:last-child {
  color: #ff3151;
}

.ai-provider-empty {
  grid-column: 1 / -1;
  min-height: 320px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #666;
  color: #888;
  text-align: center;
}

.ai-provider-empty strong {
  color: #fff;
  font-size: 20px;
}

.ai-provider-empty p {
  margin: 10px 0 0;
  font-size: 12px;
}

.ai-task-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  color: #fff;
  background: #151515;
}

.ai-task-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.ai-task-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.ai-task-heading h2 span {
  color: #14cbea;
}

.ai-task-heading > p {
  margin: 0 0 5px;
  color: #ccc;
  font-size: 15px;
  line-height: 1.75;
}

.ai-task-status {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) 150px;
  gap: 12px;
  margin-top: 44px;
}

.ai-task-status > div,
.ai-task-status button {
  min-height: 78px;
  padding: 18px;
  border: 1px solid #555;
  background: #1c1c1c;
}

.ai-task-status span,
.ai-task-status strong {
  display: block;
}

.ai-task-status span {
  color: #aaa;
  font-size: 11px;
  font-weight: 850;
}

.ai-task-status strong {
  margin-top: 8px;
  color: #14cbea;
  font-size: 30px;
  letter-spacing: -0.04em;
}

.ai-task-status button {
  color: #111;
  background: #14cbea;
  font-weight: 900;
  cursor: pointer;
}

.ai-task-status button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.ai-task-list {
  min-height: 380px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
  margin-top: 34px;
}

.ai-task-card {
  min-width: 0;
  padding: 20px;
  border: 1px solid #555;
  background: #1b1b1b;
  box-shadow: inset 0 4px 0 #14cbea;
}

.ai-task-card-top,
.ai-task-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-task-card-top {
  justify-content: space-between;
}

.ai-task-card-top span,
.ai-task-meta span {
  padding: 4px 8px;
  border: 1px solid #14cbea;
  color: #14cbea;
  font-size: 10px;
  font-weight: 850;
}

.ai-task-card-top .ai-task-status-success {
  border-color: #0de0c0;
  color: #0de0c0;
}

.ai-task-card-top .ai-task-status-running,
.ai-task-card-top .ai-task-status-pending {
  border-color: #ffb21c;
  color: #ffb21c;
}

.ai-task-card-top .ai-task-status-failed,
.ai-task-card-top .ai-task-status-canceled {
  border-color: #ff3151;
  color: #ff3151;
}

.ai-task-card h3 {
  margin: 16px 0 8px;
  overflow: hidden;
  font-size: 25px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-task-card p {
  min-height: 72px;
  max-height: 96px;
  margin: 0 0 14px;
  overflow: hidden;
  color: #ccc;
  font-size: 12px;
  line-height: 1.65;
}

.ai-task-card small {
  display: block;
  margin-top: 12px;
  overflow: hidden;
  color: #ff8aa0;
  font-size: 11px;
  line-height: 1.6;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-task-empty {
  grid-column: 1 / -1;
  min-height: 320px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #666;
  color: #888;
  text-align: center;
}

.ai-task-empty strong {
  color: #fff;
  font-size: 20px;
}

.ai-task-empty p {
  margin: 10px 0 0;
  font-size: 12px;
}

.export-section {
  min-height: 100%;
  padding: 72px clamp(38px, 6vw, 90px) 100px;
  background: #f5f3ef;
}

.export-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.42fr);
  align-items: end;
  gap: 50px;
}

.export-heading h2 {
  margin: 18px 0 0;
  font-size: clamp(48px, 6vw, 88px);
  letter-spacing: -0.065em;
  line-height: 0.92;
}

.export-heading h2 span {
  color: #14cbea;
}

.export-heading > p {
  margin: 0 0 5px;
  color: #3f4352;
  font-size: 15px;
  line-height: 1.75;
}

.export-panel {
  margin-top: 48px;
  padding: 24px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 10px 10px 0 #14cbea;
}

.export-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.export-field {
  min-width: 0;
}

.export-field > span {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

:deep(.export-field .el-select),
:deep(.export-field .el-input) {
  width: 100%;
}

:deep(.export-field .el-select__wrapper),
:deep(.export-field .el-input__wrapper) {
  min-height: 52px;
  border: 1px solid #111;
  border-radius: 0;
  box-shadow: none;
}

.export-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #111;
}

.export-options label {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
  border: 1px solid #111;
  font-size: 12px;
  font-weight: 800;
}

.export-options label.option-disabled {
  color: #777;
  background: #f2f2f2;
}

.export-options label small {
  color: #666;
  font-size: 10px;
  font-weight: 850;
}

.export-options button {
  min-height: 48px;
  min-width: 170px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 20px;
  border: 1px solid #111;
  border-radius: 999px;
  background: #14cbea;
  color: #111;
  font-weight: 900;
  cursor: pointer;
}

.export-options button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.export-options .export-preview-button {
  min-width: 145px;
  border-radius: 0;
  background: #fff;
}

.export-preview {
  min-height: 220px;
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px solid #111;
}

.export-preview-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.export-preview-heading span,
.export-preview-heading strong,
.export-preview-heading p {
  display: block;
}

.export-preview-heading span {
  color: #666;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.export-preview-heading strong {
  margin-top: 6px;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.export-preview-heading p {
  margin: 0;
  color: #666;
  font-size: 12px;
  font-weight: 800;
}

.export-preview-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.export-preview-stats > div {
  min-width: 0;
  padding: 13px;
  border: 1px solid #111;
  background: #f7f7f7;
}

.export-preview-stats span,
.export-preview-stats strong {
  display: block;
}

.export-preview-stats span {
  color: #666;
  font-size: 10px;
  font-weight: 850;
}

.export-preview-stats strong {
  margin-top: 5px;
  font-size: 26px;
  letter-spacing: -0.04em;
}

.export-preview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.export-preview-grid section,
.export-preview-related {
  min-width: 0;
  border: 1px solid #111;
  background: #fff;
}

.export-preview-grid section {
  padding: 16px;
}

.export-preview-grid h3,
.export-preview-related h3 {
  margin: 0;
  font-size: 18px;
  letter-spacing: -0.03em;
}

.export-preview-grid ul,
.export-preview-related ul {
  max-height: 220px;
  overflow: auto;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.export-preview-grid li,
.export-preview-related li {
  display: grid;
  gap: 3px;
  padding: 10px 0;
  border-top: 1px solid #ddd;
  color: #111;
  font-size: 12px;
  line-height: 1.45;
}

.export-preview-grid li:first-child,
.export-preview-related li:first-child {
  border-top: 0;
}

.export-preview-grid li strong,
.export-preview-grid li span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.export-preview-grid li span {
  color: #666;
  font-size: 11px;
}

.export-preview-grid p,
.export-preview-related p,
.export-preview-empty p {
  margin: 12px 0 0;
  color: #666;
  font-size: 12px;
  line-height: 1.65;
}

.export-preview-related {
  margin-top: 12px;
  padding: 16px;
}

.export-preview-related-title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #111;
}

.export-preview-related-title strong {
  font-size: 26px;
  letter-spacing: -0.04em;
}

.export-preview-related-title span {
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.export-preview-related article {
  display: grid;
  grid-template-columns: minmax(220px, 0.35fr) minmax(0, 1fr);
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid #ddd;
}

.export-preview-related article:first-of-type {
  border-top: 0;
}

.export-preview-related article > div > span {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 4px 8px;
  border: 1px solid #111;
  background: #ffef5a;
  font-size: 10px;
  font-weight: 900;
}

.export-preview-empty {
  min-height: 190px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.export-preview-empty strong {
  color: #111;
  font-size: 20px;
}

.export-records {
  margin-top: 56px;
}

.export-record-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #111;
}

.export-record-heading strong,
.export-record-heading span {
  display: block;
}

.export-record-heading strong {
  font-size: 40px;
  letter-spacing: -0.06em;
}

.export-record-heading span {
  color: #666;
  font-size: 10px;
  font-weight: 850;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.export-record-heading button,
.export-record-card button {
  min-height: 42px;
  border: 1px solid #111;
  background: #fff;
  font-weight: 850;
  cursor: pointer;
}

.export-record-grid {
  min-height: 220px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.export-record-card {
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  border: 1px solid #111;
  background: #fff;
  box-shadow: 8px 8px 0 #111;
}

.export-record-card span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid #111;
  background: #ffef5a;
  font-size: 11px;
  font-weight: 900;
}

.export-record-card h3 {
  margin: 14px 0 8px;
  overflow: hidden;
  font-size: 25px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.export-record-card p {
  margin: 0;
  color: #666;
  font-size: 12px;
}

.export-record-card button {
  flex: 0 0 auto;
  padding: 0 16px;
  background: #14cbea;
}

.export-empty {
  grid-column: 1 / -1;
  min-height: 220px;
  display: grid;
  place-content: center;
  justify-items: center;
  border: 1px dashed #777;
  color: #666;
  text-align: center;
}

.export-empty strong {
  color: #111;
  font-size: 20px;
}

.export-empty p {
  margin: 10px 0 0;
  font-size: 12px;
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

.batch-upload-list {
  display: grid;
  gap: 16px;
  margin-bottom: 22px;
}

.batch-upload-card {
  min-width: 0;
  padding: 18px;
  border: 1px solid #111;
  background: #faf9f6;
}

.batch-upload-card :global(.el-form-item:last-child) {
  margin-bottom: 0;
}

.batch-upload-card-heading {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #111;
}

.batch-upload-card-heading > span {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid #111;
  border-radius: 50%;
  background: var(--dialog-accent);
  font-size: 12px;
  font-weight: 900;
}

.batch-upload-card-heading strong,
.batch-upload-card-heading p {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-upload-card-heading strong {
  font-size: 16px;
}

.batch-upload-card-heading p {
  margin: 4px 0 0;
  color: #666;
  font-size: 11px;
  font-weight: 800;
}

.text-preview {
  min-height: 240px;
  max-height: 62vh;
  overflow: auto;
  padding: 4px 12px 4px 0;
  scrollbar-color: #14cbea #eee;
  scrollbar-width: thin;
}

.text-page {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 20px;
  padding: 24px 0 30px;
  border-bottom: 1px solid #111;
}

.text-page:first-child {
  padding-top: 0;
}

.text-page-marker span {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border: 1px solid #111;
  border-radius: 50%;
  background: #14cbea;
  font-size: 12px;
  font-weight: 900;
}

.text-page-content {
  min-width: 0;
}

.text-page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ddd;
  color: #555;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.text-page-heading strong {
  color: #111;
  font-size: 15px;
}

.text-page-body {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #202020;
  font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
  font-size: 16px;
  line-height: 1.95;
  letter-spacing: 0.015em;
}

.text-dialog-footer {
  align-items: center;
  justify-content: space-between;
}

.text-dialog-footer > span {
  color: #666;
  font-size: 12px;
  font-weight: 800;
}

.similar-dialog {
  display: grid;
  gap: 14px;
}

.similar-dialog > p {
  margin: 0;
  color: #555;
  line-height: 1.7;
}

.similar-material-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #111;
  background: #faf9f6;
}

.similar-material-card > div:first-child {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.similar-material-card strong,
.similar-material-card span {
  display: block;
}

.similar-material-card strong {
  min-width: 0;
  overflow: hidden;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.similar-material-card > div:first-child span {
  flex: 0 0 auto;
  color: #666;
  font-size: 11px;
  font-weight: 850;
}

.similar-material-card p {
  margin: 0;
  color: #555;
  font-size: 13px;
  line-height: 1.65;
}

.similar-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.similar-reasons span {
  padding: 5px 9px;
  border: 1px solid #111;
  background: #ffef5a;
  color: #111;
  font-size: 11px;
  font-weight: 850;
}

.similar-material-card button {
  justify-self: start;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #111;
  background: #fff;
  font-weight: 850;
  cursor: pointer;
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

:global(.studio-dialog-overlay) {
  background: rgba(10, 10, 10, 0.56);
  backdrop-filter: blur(9px);
}

:global(.studio-dialog) {
  --dialog-accent: #0de0c0;
  max-width: calc(100vw - 36px);
  margin-top: 7vh !important;
  overflow: hidden;
  border: 2px solid #111;
  border-radius: 0;
  background: #fff;
  box-shadow: 16px 16px 0 var(--dialog-accent);
}

:global(.studio-dialog-course) {
  --dialog-accent: #ffb21c;
}

:global(.studio-dialog-chapter) {
  --dialog-accent: #0de0c0;
}

:global(.studio-dialog-material) {
  --dialog-accent: #14cbea;
}

:global(.studio-dialog-delete) {
  --dialog-accent: #ff3151;
}

:global(.studio-dialog-text) {
  --dialog-accent: #14cbea;
}

:global(.studio-dialog-similar) {
  --dialog-accent: #ffef5a;
}

:global(.studio-dialog-text .el-dialog__body) {
  background: #faf9f6;
}

:global(.studio-dialog .el-dialog__header) {
  position: relative;
  margin: 0;
  padding: 28px 30px 24px;
  border-bottom: 2px solid #111;
}

:global(.studio-dialog .el-dialog__header::after) {
  content: "";
  position: absolute;
  right: 82px;
  bottom: -18px;
  width: 36px;
  height: 36px;
  border: 2px solid #111;
  border-radius: 50%;
  background: var(--dialog-accent);
}

:global(.studio-dialog .el-dialog__body) {
  padding: 30px;
}

:global(.studio-dialog .el-dialog__footer) {
  padding: 0 30px 30px;
}

.dialog-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.dialog-heading p {
  margin: 0 0 9px;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.dialog-heading h2 {
  margin: 0;
  font-size: clamp(30px, 4vw, 46px);
  font-weight: 850;
  letter-spacing: -0.065em;
  line-height: 0.98;
}

.dialog-heading h2 span {
  color: #ff3151;
}

.dialog-close {
  width: 42px;
  height: 42px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  padding: 0 0 4px;
  border: 1px solid #111;
  border-radius: 50%;
  background: #fff;
  font-size: 29px;
  font-weight: 300;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.25s, background 0.25s, color 0.25s;
}

.dialog-close:hover {
  background: #111;
  color: #fff;
  transform: rotate(90deg);
}

:global(.studio-dialog .studio-form .el-form-item) {
  margin-bottom: 22px;
}

:global(.studio-dialog .studio-form .el-form-item__label) {
  padding-bottom: 8px;
  color: #111;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

:global(.studio-dialog .el-input__wrapper),
:global(.studio-dialog .el-select__wrapper),
:global(.studio-dialog .el-textarea__inner),
:global(.studio-dialog .el-input-number .el-input__wrapper) {
  min-height: 48px;
  border: 1px solid #111;
  border-radius: 0;
  background: #fff;
  box-shadow: none;
  transition: box-shadow 0.2s, background 0.2s;
}

:global(.studio-dialog .el-textarea__inner) {
  padding: 13px 15px;
  line-height: 1.65;
}

:global(.studio-dialog .el-input__wrapper.is-focus),
:global(.studio-dialog .el-select__wrapper.is-focused),
:global(.studio-dialog .el-textarea__inner:focus),
:global(.studio-dialog .el-input-number .el-input__wrapper.is-focus) {
  background: #fffef8;
  box-shadow: inset 0 -4px 0 var(--dialog-accent);
}

:global(.studio-dialog .el-input-number) {
  width: 100%;
}

:global(.studio-dialog .el-input-number__decrease),
:global(.studio-dialog .el-input-number__increase) {
  width: 44px;
  border-color: #111;
  background: #f4f2ee;
  color: #111;
}

:global(.studio-dialog .el-switch) {
  --el-switch-on-color: var(--dialog-accent);
  --el-switch-off-color: #d8d5cf;
  height: 48px;
}

:global(.studio-dialog .el-switch__core) {
  border: 1px solid #111;
}

.field-hint {
  width: 100%;
  margin: 7px 0 0;
  color: #777;
  font-size: 11px;
}

.studio-upload {
  width: 100%;
}

:global(.studio-dialog .studio-upload .el-upload) {
  width: 100%;
}

:global(.studio-dialog .studio-upload .el-upload-dragger) {
  width: 100%;
  min-height: 168px;
  display: grid;
  place-content: center;
  justify-items: center;
  padding: 24px;
  border: 1px dashed #111;
  border-radius: 0;
  background:
    linear-gradient(135deg, transparent 0 72%, color-mix(in srgb, var(--dialog-accent) 28%, transparent) 72%),
    #faf9f6;
  transition: background 0.2s, transform 0.2s;
}

:global(.studio-dialog .studio-upload .el-upload-dragger:hover) {
  border-color: #111;
  background:
    linear-gradient(135deg, transparent 0 65%, color-mix(in srgb, var(--dialog-accent) 45%, transparent) 65%),
    #fff;
  transform: translateY(-2px);
}

.upload-symbol {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  margin-bottom: 13px;
  border: 1px solid #111;
  border-radius: 50%;
  background: var(--dialog-accent);
  color: #111;
  font-size: 25px;
}

.studio-upload strong {
  font-size: 16px;
}

.studio-upload p {
  margin: 7px 0 0;
  color: #777;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.11em;
}

:global(.studio-dialog .el-upload-list__item) {
  border: 1px solid #111;
  border-radius: 0;
  background: #fff;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 22px;
  border-top: 1px solid #111;
}

.dialog-button {
  min-height: 48px;
  padding: 0 22px;
  border: 1px solid #111;
  border-radius: 999px;
  font-weight: 850;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.dialog-button:hover:not(:disabled) {
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 #111;
}

.dialog-button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.dialog-button-ghost {
  background: #fff;
}

.dialog-button-danger {
  margin-right: auto;
  background: #ff3151;
  color: #fff;
}

.dialog-button-danger-confirm {
  min-width: 168px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: #ff3151;
  color: #fff;
}

.dialog-button-danger-confirm strong {
  font-size: 22px;
}

.delete-dialog-copy strong {
  display: block;
  overflow: hidden;
  font-size: 25px;
  letter-spacing: -0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-dialog-copy p {
  margin: 18px 0;
  color: #555;
  line-height: 1.7;
}

.delete-warning {
  padding: 14px 16px;
  border: 1px solid #111;
  background: #fff0f3;
  font-size: 12px;
  font-weight: 800;
}

.danger-pill {
  border-color: #ff3151;
  color: #ff3151;
}

.danger-pill:hover {
  background: #ff3151;
  color: #fff;
}

.dialog-button-primary {
  min-width: 168px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: var(--dialog-accent);
}

.dialog-button-primary strong {
  font-size: 20px;
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

  .course-type-distribution {
    grid-template-columns: 1fr;
  }

  .type-distribution-heading {
    border-right: 0;
    border-bottom: 1px solid #111;
    padding-bottom: 14px;
  }

  .chapter-track {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  :global(.studio-dialog) {
    width: calc(100vw - 24px) !important;
    max-height: calc(100vh - 34px);
    margin: 17px auto !important;
    overflow-y: auto;
    box-shadow: 8px 8px 0 var(--dialog-accent);
  }

  :global(.studio-dialog .el-dialog__header) {
    padding: 23px 20px 20px;
  }

  :global(.studio-dialog .el-dialog__header::after) {
    right: 66px;
    width: 28px;
    height: 28px;
  }

  :global(.studio-dialog .el-dialog__body) {
    padding: 24px 20px;
  }

  :global(.studio-dialog .el-dialog__footer) {
    padding: 0 20px 22px;
  }

  .dialog-heading h2 {
    font-size: 31px;
  }

  .dialog-footer {
    display: grid;
    grid-template-columns: 0.7fr 1.3fr;
  }

  .dialog-button {
    width: 100%;
    padding: 0 16px;
  }

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
  .relation-section,
  .chapter-section,
  .material-section,
  .gap-section,
  .teacher-section,
  .review-section,
  .ai-config-section,
  .ai-task-section,
  .export-section {
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

  .type-distribution-row {
    grid-template-columns: 78px minmax(0, 1fr) 34px;
  }

  .section-intro,
  .relation-heading,
  .material-title-row,
  .search-heading,
  .knowledge-heading,
  .gap-heading,
  .teacher-heading,
  .review-heading,
  .ai-config-heading,
  .ai-task-heading,
  .export-heading {
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

  .search-heading > p {
    margin-top: 24px;
  }

  .knowledge-heading > p {
    margin-top: 24px;
  }

  .gap-heading > p {
    margin-top: 24px;
  }

  .teacher-heading > p {
    margin-top: 24px;
  }

  .review-heading > p {
    margin-top: 24px;
  }

  .ai-config-heading > p {
    margin-top: 24px;
  }

  .ai-task-heading > p {
    margin-top: 24px;
  }

  .relation-heading > p {
    margin-top: 24px;
  }

  .export-heading > p {
    margin-top: 24px;
  }

  .relation-form,
  .relation-groups,
  .search-input-row,
  .search-filters,
  .search-history-list,
  .knowledge-generator-main,
  .knowledge-tags-panel,
  .knowledge-mastery-row,
  .gap-generator,
  .gap-layout,
  .gap-summary,
  .gap-item-grid,
  .teacher-layout,
  .teacher-edit-grid,
  .teacher-profile-grid,
  .review-layout,
  .review-profile-list,
  .ai-config-status,
  .ai-config-layout,
  .ai-provider-list,
  .ai-task-status,
  .ai-task-list,
  .export-form-grid,
  .export-preview-stats,
  .export-preview-grid,
  .export-preview-related article,
  .export-record-grid {
    grid-template-columns: 1fr;
  }

  .search-result-card {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .search-result-actions {
    grid-column: 2;
    display: flex;
    justify-items: start;
  }

  .knowledge-grid {
    grid-template-columns: 1fr;
  }

  .export-options,
  .knowledge-mastery-actions,
  .teacher-edit-actions,
  .review-form-actions,
  .ai-provider-actions,
  .export-preview-heading,
  .export-record-heading,
  .export-record-card {
    display: grid;
    justify-items: stretch;
  }

  .knowledge-generator > p {
    display: none;
  }

  .material-groups {
    margin-top: 40px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
