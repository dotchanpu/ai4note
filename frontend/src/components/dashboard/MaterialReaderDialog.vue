<template>
  <el-dialog
    :model-value="modelValue"
    class="studio-dialog studio-dialog-text material-reader-dialog"
    modal-class="studio-dialog-overlay"
    width="1040px"
    :show-close="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="dialog-heading">
        <div>
          <p>reader / 资料阅读器</p>
          <h2>{{ material?.title || '资料预览' }}<span>.</span></h2>
        </div>
        <button type="button" class="dialog-close" aria-label="关闭" @click="$emit('update:modelValue', false)">
          ×
        </button>
      </div>
    </template>

    <div v-loading="loading" class="text-preview material-reader">
      <iframe
        v-if="mode === 'pdf'"
        class="material-reader-frame"
        :src="readerUrl"
        title="资料 PDF 预览"
      ></iframe>

      <article v-else-if="mode === 'markdown'" class="material-reader-markdown">
        <div v-html="renderMarkdown(content)"></div>
      </article>

      <article v-else-if="mode === 'text'" class="material-reader-plain">
        <pre>{{ content }}</pre>
      </article>

      <div v-else-if="mode === 'chunks'" class="material-reader-chunks">
        <section v-for="(chunk, index) in chunks" :key="chunk.id || index" class="text-page">
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
        <el-empty v-if="!loading && chunks.length === 0" description="暂无解析文本" />
      </div>

      <div v-else class="material-reader-download">
        <strong>该类型暂不支持网页内直接预览</strong>
        <p>可以下载后用本地阅读器打开：{{ material?.originalName || material?.title }}</p>
        <button type="button" @click="$emit('download')">下载资料</button>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer text-dialog-footer">
        <span>{{ footerText }}</span>
        <button
          v-if="canDownload"
          type="button"
          class="dialog-button dialog-button-ghost"
          @click="$emit('download')"
        >
          下载原文件
        </button>
        <button type="button" class="dialog-button dialog-button-primary" @click="$emit('update:modelValue', false)">
          <span>阅读完成</span>
          <strong>→</strong>
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  material: {
    type: Object,
    default: null
  },
  mode: {
    type: String,
    default: 'text'
  },
  loading: {
    type: Boolean,
    default: false
  },
  chunks: {
    type: Array,
    default: () => []
  },
  content: {
    type: String,
    default: ''
  },
  readerUrl: {
    type: String,
    default: ''
  },
  footerText: {
    type: String,
    default: '未选择资料'
  },
  canDownload: {
    type: Boolean,
    default: false
  },
  renderMarkdown: {
    type: Function,
    required: true
  }
})

defineEmits(['update:modelValue', 'download'])
</script>

<style scoped>
.dialog-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 20px;
}

.dialog-heading p {
  margin: 0 0 10px;
  color: #777;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.dialog-heading h2 {
  margin: 0;
  color: #111;
  font-size: 36px;
  letter-spacing: -0.06em;
  line-height: 0.96;
}

.dialog-heading h2 span {
  color: #ff3151;
}

.dialog-close {
  width: 42px;
  height: 42px;
  border: 1px solid #111;
  border-radius: 50%;
  background: #fff;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}

.dialog-close:hover {
  background: #ffef5a;
  transform: rotate(8deg);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-button {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 18px;
  border: 1px solid #111;
  background: #fff;
  font-weight: 900;
  cursor: pointer;
}

.dialog-button:hover:not(:disabled) {
  transform: translate(-1px, -1px);
  box-shadow: 4px 4px 0 #111;
}

.dialog-button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.dialog-button-ghost {
  background: #f7f5ef;
}

.dialog-button-primary {
  background: #111;
  color: #fff;
}

.dialog-button-primary strong {
  color: #ffef5a;
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

.material-reader {
  min-height: 520px;
  padding: 0;
  border: 1px solid #111;
  background: #fff;
}

.material-reader-frame {
  width: 100%;
  min-height: 64vh;
  display: block;
  border: 0;
  background: #f5f5f2;
}

.material-reader-markdown,
.material-reader-plain,
.material-reader-download,
.material-reader-chunks {
  padding: 28px;
}

.material-reader-markdown {
  color: #171717;
  font-size: 15px;
  line-height: 1.8;
}

.material-reader-markdown :deep(h1),
.material-reader-markdown :deep(h2),
.material-reader-markdown :deep(h3),
.material-reader-markdown :deep(h4) {
  margin: 1.25em 0 0.55em;
  letter-spacing: -0.03em;
  line-height: 1.15;
}

.material-reader-markdown :deep(h1:first-child),
.material-reader-markdown :deep(h2:first-child),
.material-reader-markdown :deep(h3:first-child) {
  margin-top: 0;
}

.material-reader-markdown :deep(h1) {
  font-size: 34px;
}

.material-reader-markdown :deep(h2) {
  padding-bottom: 8px;
  border-bottom: 1px solid #111;
  font-size: 26px;
}

.material-reader-markdown :deep(h3) {
  font-size: 21px;
}

.material-reader-markdown :deep(p),
.material-reader-markdown :deep(li) {
  color: #333;
}

.material-reader-markdown :deep(ul) {
  display: grid;
  gap: 8px;
  padding-left: 22px;
}

.material-reader-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 18px 0;
  font-size: 13px;
}

.material-reader-markdown :deep(th),
.material-reader-markdown :deep(td) {
  padding: 10px 12px;
  border: 1px solid #111;
  text-align: left;
  vertical-align: top;
}

.material-reader-markdown :deep(th) {
  background: #ffef5a;
  font-weight: 900;
}

.material-reader-plain pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #222;
  font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
  font-size: 15px;
  line-height: 1.85;
}

.material-reader-download {
  min-height: 420px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.material-reader-download strong {
  font-size: 22px;
}

.material-reader-download p {
  max-width: 520px;
  margin: 0;
  color: #555;
  line-height: 1.7;
}

.material-reader-download button {
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid #111;
  border-radius: 999px;
  background: #111;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.text-dialog-footer {
  align-items: center;
  justify-content: space-between;
}
</style>
