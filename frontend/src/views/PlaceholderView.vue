<template>
  <section class="page placeholder-page">
    <div class="page-panel placeholder-panel">
      <ElEmpty :description="`${title}页面准备中`" />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta.title || '当前')
</script>

<style scoped>
.placeholder-panel {
  display: grid;
  min-height: 420px;
  place-items: center;
}
</style>
