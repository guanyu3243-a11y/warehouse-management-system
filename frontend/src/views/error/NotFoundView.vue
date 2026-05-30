<template>
  <section class="page error-page">
    <div class="error-content">
      <ElIcon class="error-icon not-found-icon"><Search /></ElIcon>
      <p class="error-code">404</p>
      <h1>页面不存在</h1>
      <p class="error-copy">访问的页面不存在或已经移动，请返回 Dashboard 继续操作。</p>
      <ElButton type="primary" @click="goDashboard">
        <ElIcon><House /></ElIcon>
        返回 Dashboard
      </ElButton>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { House, Search } from '@element-plus/icons-vue'

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
  color: var(--wms-accent);
  font-size: 48px;
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
