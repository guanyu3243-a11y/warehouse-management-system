<template>
  <main class="login-page">
    <section class="login-shell">
      <aside class="login-visual" aria-label="服装仓库管理系统">
        <div class="visual-brand">
          <div class="brand-mark">
            <img src="/youbao-logo.jpg" alt="友宝 logo" />
          </div>
          <div>
            <strong>服装仓库管理系统</strong>
          </div>
        </div>

        <div class="rack-scene" aria-hidden="true">
          <div class="rack-header">
            <span />
            <span />
            <span />
          </div>
          <div class="rack-grid">
            <span class="box tall" />
            <span class="box" />
            <span class="box muted" />
            <span class="box amber" />
            <span class="box wide" />
            <span class="box" />
            <span class="box muted" />
            <span class="box tall amber" />
            <span class="box wide muted" />
          </div>
          <div class="rack-footer">
            <span />
            <span />
          </div>
        </div>

        <div class="visual-status">
          <span>商品</span>
          <span>库存</span>
          <span>追溯</span>
        </div>
      </aside>

      <section class="login-panel">
        <div class="login-brand">
          <div class="brand-mark">
            <img src="/youbao-logo.jpg" alt="友宝 logo" />
          </div>
          <div>
            <h1>服装仓库管理系统</h1>
            <p>Warehouse Management System</p>
          </div>
        </div>

        <div class="login-title">
          <h2>登录工作台</h2>
          <p>请输入账号信息继续</p>
        </div>

        <ElForm
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <ElFormItem label="用户名" prop="username">
            <ElInput
              v-model.trim="loginForm.username"
              autocomplete="username"
              placeholder="请输入用户名"
            >
              <template #prefix>
                <ElIcon><User /></ElIcon>
              </template>
            </ElInput>
          </ElFormItem>

          <ElFormItem label="密码" prop="password">
            <ElInput
              v-model="loginForm.password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              type="password"
            >
              <template #prefix>
                <ElIcon><Lock /></ElIcon>
              </template>
            </ElInput>
          </ElFormItem>

          <ElButton
            class="login-button"
            type="primary"
            :icon="Key"
            :loading="authStore.loading"
            @click="handleLogin"
          >
            登录
          </ElButton>
        </ElForm>
      </section>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Lock, User } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref()

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    {
      required: true,
      message: '请输入用户名',
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur'
    },
    {
      min: 6,
      message: '密码至少 6 位',
      trigger: 'blur'
    }
  ]
}

async function handleLogin() {
  if (!loginFormRef.value) {
    return
  }

  await loginFormRef.value.validate()
  await authStore.login(loginForm)

  ElMessage.success('登录成功')
  router.push(route.query.redirect || '/dashboard')
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at 18% 18%, rgba(15, 118, 110, 0.16), transparent 28%),
    radial-gradient(circle at 84% 76%, rgba(183, 121, 31, 0.1), transparent 24%),
    linear-gradient(135deg, #f7faf9 0%, #eef4f1 48%, #f8faf9 100%);
}

.login-page::before {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(18, 32, 31, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 32, 31, 0.04) 1px, transparent 1px);
  background-size: 28px 28px;
  content: "";
}

.login-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(380px, 440px);
  width: min(1040px, 100%);
  min-height: 640px;
  overflow: hidden;
  border: 1px solid rgba(219, 229, 225, 0.86);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 28px 90px rgba(18, 32, 31, 0.18);
  backdrop-filter: blur(16px);
}

.login-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 640px;
  overflow: hidden;
  padding: 36px;
  color: #f8fafc;
  background:
    radial-gradient(circle at 18% 22%, rgba(45, 212, 191, 0.16), transparent 30%),
    radial-gradient(circle at 86% 72%, rgba(226, 178, 75, 0.1), transparent 26%),
    linear-gradient(130deg, #0b4f49 0%, #0f6f66 38%, #102826 100%);
  background-size: 150% 150%, 130% 130%, 220% 220%;
  animation: loginVisualSurface 18s ease-in-out infinite alternate;
}

.login-visual::before {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(115deg, transparent 0 42%, rgba(255, 255, 255, 0.08) 48%, transparent 56% 100%);
  background-position: 0 0, 0 0, -120% 0;
  background-size: 34px 34px, 34px 34px, 240% 100%;
  content: "";
  mask-image: linear-gradient(180deg, black, transparent 78%);
  animation: loginVisualScan 14s ease-in-out infinite;
}

.login-visual::after {
  position: absolute;
  right: -120px;
  bottom: -140px;
  width: 360px;
  height: 360px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  content: "";
  animation: loginVisualOrbit 16s ease-in-out infinite alternate;
}

@keyframes loginVisualSurface {
  0% {
    background-position: 0% 0%, 100% 100%, 0% 50%;
  }

  100% {
    background-position: 18% 10%, 74% 60%, 100% 50%;
  }
}

@keyframes loginVisualScan {
  0%,
  24% {
    background-position: 0 0, 0 0, -120% 0;
  }

  70%,
  100% {
    background-position: 12px 8px, 12px 8px, 120% 0;
  }
}

@keyframes loginVisualOrbit {
  0% {
    opacity: 0.55;
    transform: translate3d(0, 0, 0) scale(1);
  }

  100% {
    opacity: 0.85;
    transform: translate3d(-18px, -14px, 0) scale(1.04);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-visual,
  .login-visual::before,
  .login-visual::after {
    animation: none;
  }
}

.visual-brand,
.rack-scene,
.visual-status {
  position: relative;
  z-index: 1;
}

.visual-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.visual-brand strong,
.visual-brand span {
  display: block;
}

.visual-brand strong {
  font-size: 17px;
  line-height: 1.3;
}

.visual-brand span {
  margin-top: 4px;
  color: #bcd4cf;
  font-size: 12px;
}

.rack-scene {
  width: min(480px, 100%);
  margin: 54px auto 0;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.rack-header,
.rack-footer {
  display: flex;
  gap: 10px;
}

.rack-header span,
.rack-footer span {
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.34);
}

.rack-header span:nth-child(1) {
  width: 36%;
}

.rack-header span:nth-child(2) {
  width: 22%;
}

.rack-header span:nth-child(3) {
  width: 18%;
}

.rack-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 58px;
  gap: 12px;
  margin: 18px 0;
}

.box {
  border-radius: 8px;
  background: linear-gradient(145deg, #dff7f2, #87c9c1);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.18);
}

.box.muted {
  background: linear-gradient(145deg, #d7dde2, #a9b4bd);
}

.box.amber {
  background: linear-gradient(145deg, #f5d489, #c88b25);
}

.box.tall {
  grid-row: span 2;
}

.box.wide {
  grid-column: span 2;
}

.rack-footer span:nth-child(1) {
  width: 48%;
}

.rack-footer span:nth-child(2) {
  width: 26%;
}

.visual-status {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.visual-status span {
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  padding: 7px 12px;
  color: #dff7f2;
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  font-weight: 700;
}

.login-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 100%;
  border-left: 1px solid #e6eeeb;
  padding: 46px;
  background: #ffffff;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 30px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  overflow: hidden;
  padding: 5px;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(15, 118, 110, 0.24);
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.login-brand h1 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 21px;
  line-height: 1.3;
}

.login-brand p {
  margin: 4px 0 0;
  color: var(--wms-muted);
  font-size: 13px;
}

.login-title {
  margin-bottom: 22px;
}

.login-title h2 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0;
}

.login-title p {
  margin: 8px 0 0;
  color: var(--wms-muted);
  font-size: 14px;
}

.login-form {
  display: grid;
  gap: 4px;
}

.login-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 42px;
}

.login-button {
  width: 100%;
  height: 42px;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .login-page {
    padding: 20px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .login-visual {
    min-height: 260px;
    padding: 24px;
  }

  .rack-scene {
    display: none;
  }

  .login-panel {
    border-left: 0;
    padding: 30px;
  }
}

@media (max-width: 520px) {
  .login-brand {
    align-items: flex-start;
  }

  .login-title h2 {
    font-size: 23px;
  }
}
</style>
