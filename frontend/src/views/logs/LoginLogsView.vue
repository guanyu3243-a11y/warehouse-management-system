<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>登录日志</h1>
        <p>查看用户登录成功、失败、锁定限制等安全审计记录。</p>
      </div>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElInput
          v-model.trim="queryForm.username"
          class="filter-keyword"
          clearable
          placeholder="用户名"
          @keyup.enter="handleSearch"
        />
        <ElSelect v-model="queryForm.success" clearable placeholder="登录结果">
          <ElOption label="成功" :value="true" />
          <ElOption label="失败" :value="false" />
        </ElSelect>
        <ElDatePicker
          v-model="timeRange"
          end-placeholder="结束时间"
          range-separator="至"
          start-placeholder="开始时间"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
        <ElButton type="primary" @click="handleSearch">
          <ElIcon><Search /></ElIcon>
          查询
        </ElButton>
        <ElButton @click="handleReset">
          <ElIcon><Refresh /></ElIcon>
          重置
        </ElButton>
      </div>

      <div class="table-wrap">
        <ElTable v-loading="loading" :data="records" row-key="id">
          <ElTableColumn prop="id" label="ID" width="80" />
          <ElTableColumn prop="userId" label="用户 ID" width="100" />
          <ElTableColumn prop="username" label="用户名" min-width="140" show-overflow-tooltip />
          <ElTableColumn label="结果" width="90">
            <template #default="{ row }">
              <ElTag :type="row.success ? 'success' : 'danger'" effect="plain">
                {{ row.success ? '成功' : '失败' }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
          <ElTableColumn prop="requestIp" label="IP" min-width="130" show-overflow-tooltip />
          <ElTableColumn prop="userAgent" label="User-Agent" min-width="260" show-overflow-tooltip />
          <ElTableColumn label="时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ElTableColumn>
        </ElTable>

        <div class="pagination-row">
          <ElPagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[10, 20, 50]"
            :total="pagination.total"
            @current-change="loadList"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'

import { loginLogApi } from '@/api/business'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const timeRange = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  username: '',
  success: undefined
})

async function loadList() {
  loading.value = true

  try {
    const result = await loginLogApi.page({
      page: pagination.page,
      size: pagination.size,
      username: queryForm.username,
      success: queryForm.success,
      startTime: timeRange.value?.[0],
      endTime: timeRange.value?.[1]
    })

    records.value = result.records || []
    pagination.total = result.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadList()
}

function handleReset() {
  queryForm.username = ''
  queryForm.success = undefined
  timeRange.value = []
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

onMounted(loadList)
</script>
