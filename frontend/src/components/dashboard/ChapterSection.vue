<template>
<section
            id="chapters"
            class="chapter-section scroll-panel"
            :class="{ 'section-active': active }"
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
  chapterLoading,
  chapters,
  confirmChapterDeletion,
  openChapterCreator,
  openChapterEditor
} = useDashboardContext()
</script>
