<template>
  <section class="page error-page">
    <div class="error-content">
      <ElIcon class="error-icon forbidden-icon"><Lock /></ElIcon>
      <p class="error-code">403</p>
      <h1>无权限访问</h1>
      <p class="error-copy">当前账号没有访问该模块的权限，请联系管理员调整角色或权限。</p>
      <ElButton type="primary" @click="goDashboard">
        <ElIcon><House /></ElIcon>
        返回 Dashboard
      </ElButton>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { House, Lock } from '@element-plus/icons-vue'

const router = useRouter()

function goDashboard() {
  router.push('/dashboard')
}
</script>

<style scoped>
.error-page {
  display: grid;
  min-height: calc(100vh - 64px);
  place-items: center;
}

.error-content {
  display: grid;
  max-width: 520px;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.error-icon {
  font-size: 48px;
}

.forbidden-icon {
  color: var(--wms-danger);
}

.error-code {
  margin: 4px 0 0;
  color: var(--wms-muted);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
}

h1 {
  margin: 0;
  color: var(--wms-ink);
  font-size: 30px;
}

.error-copy {
  margin: 0 0 8px;
  color: var(--wms-muted);
  line-height: 1.7;
}
</style>
