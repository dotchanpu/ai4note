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
</template>

<script setup>
import { useDashboardContext } from './DashboardContext'

const {
  currentUser,
  authMode,
  authLoading,
  authForm,
  submitAuth
} = useDashboardContext()
</script>
