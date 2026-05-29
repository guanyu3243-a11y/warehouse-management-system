<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <div class="brand-mark">仓</div>
        <div>
          <h1>服装仓库管理系统</h1>
          <p>Warehouse Management System</p>
        </div>
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
          :loading="authStore.loading"
          @click="handleLogin"
        >
          <ElIcon><Key /></ElIcon>
          登录
        </ElButton>
      </ElForm>
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
  padding: 24px;
  background:
    linear-gradient(90deg, rgba(15, 118, 110, 0.08), rgba(180, 83, 9, 0.06)),
    #f4f6f5;
}

.login-panel {
  width: min(420px, 100%);
  border: 1px solid var(--wms-border);
  border-radius: 8px;
  padding: 32px;
  background: #ffffff;
  box-shadow: 0 18px 60px rgba(31, 41, 51, 0.12);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--wms-accent);
  font-size: 18px;
  font-weight: 700;
}

.login-brand h1 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 22px;
  line-height: 1.3;
}

.login-brand p {
  margin: 4px 0 0;
  color: var(--wms-muted);
  font-size: 13px;
}

.login-form {
  display: grid;
  gap: 2px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}
</style>
