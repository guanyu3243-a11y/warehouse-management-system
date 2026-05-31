<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>修改密码</h1>
        <p>更新当前登录账号的密码，保存后请使用新密码继续登录。</p>
      </div>
    </div>

    <div class="password-panel">
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="96px">
        <ElFormItem label="旧密码" prop="oldPassword">
          <ElInput v-model="form.oldPassword" show-password type="password" autocomplete="current-password" />
        </ElFormItem>
        <ElFormItem label="新密码" prop="newPassword">
          <ElInput v-model="form.newPassword" show-password type="password" autocomplete="new-password" />
        </ElFormItem>
        <ElFormItem label="确认密码" prop="confirmPassword">
          <ElInput v-model="form.confirmPassword" show-password type="password" autocomplete="new-password" />
        </ElFormItem>
        <ElFormItem>
          <ElButton type="primary" :loading="saving" @click="submit">保存</ElButton>
          <ElButton @click="router.back()">返回</ElButton>
        </ElFormItem>
      </ElForm>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { changePasswordApi } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const saving = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '新密码长度需为 6 到 100 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value?.validate()
  saving.value = true

  try {
    await changePasswordApi({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.password-panel {
  max-width: 560px;
  border: 1px solid var(--wms-border);
  border-radius: 8px;
  padding: 24px 24px 8px;
  background: var(--wms-surface);
}
</style>
