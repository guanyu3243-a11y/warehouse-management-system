<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1>操作日志</h1>
        <p>查询用户在系统中的关键操作记录和请求信息。</p>
      </div>
    </div>

    <div class="page-panel">
      <div class="filter-bar">
        <ElInputNumber
          v-model="queryForm.userId"
          :min="1"
          controls-position="right"
          placeholder="用户 ID"
        />
        <ElSelect v-model="queryForm.module" clearable filterable placeholder="模块">
          <ElOption v-for="module in operationModuleOptions" :key="module" :label="module" :value="module" />
        </ElSelect>
        <ElSelect v-model="queryForm.action" clearable filterable placeholder="动作">
          <ElOption v-for="action in operationActionOptions" :key="action" :label="action" :value="action" />
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
          <ElTableColumn prop="module" label="模块" min-width="130" show-overflow-tooltip />
          <ElTableColumn prop="action" label="动作" min-width="110" show-overflow-tooltip />
          <ElTableColumn prop="method" label="方法" width="90" />
          <ElTableColumn prop="requestUri" label="请求地址" min-width="240" show-overflow-tooltip />
          <ElTableColumn prop="requestIp" label="IP" min-width="130" show-overflow-tooltip />
          <ElTableColumn prop="responseStatus" label="状态码" width="90" />
          <ElTableColumn prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <ElTableColumn label="时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" fixed="right" width="90">
            <template #default="{ row }">
              <ElButton link type="primary" size="small" @click="openDetail(row)">详情</ElButton>
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

    <ElDrawer v-model="detailVisible" title="日志详情" size="480px">
      <ElDescriptions v-if="currentLog" :column="1" border>
        <ElDescriptionsItem label="日志 ID">{{ currentLog.id }}</ElDescriptionsItem>
        <ElDescriptionsItem label="用户 ID">{{ currentLog.userId }}</ElDescriptionsItem>
        <ElDescriptionsItem label="模块">{{ currentLog.module }}</ElDescriptionsItem>
        <ElDescriptionsItem label="动作">{{ currentLog.action }}</ElDescriptionsItem>
        <ElDescriptionsItem label="请求方法">{{ currentLog.method }}</ElDescriptionsItem>
        <ElDescriptionsItem label="请求地址">{{ currentLog.requestUri }}</ElDescriptionsItem>
        <ElDescriptionsItem label="请求 IP">{{ currentLog.requestIp || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="响应状态">{{ currentLog.responseStatus || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="错误信息">{{ currentLog.errorMessage || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="User-Agent">{{ currentLog.userAgent || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="请求内容">{{ currentLog.requestBody || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="变更前">{{ currentLog.beforeData || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="变更后">{{ currentLog.afterData || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="描述">{{ currentLog.description || '-' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="时间">{{ formatDateTime(currentLog.createdAt) }}</ElDescriptionsItem>
      </ElDescriptions>
    </ElDrawer>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'

import { operationLogApi } from '@/api/business'
import { operationActionOptions, operationModuleOptions } from '@/constants/options'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const detailVisible = ref(false)
const currentLog = ref(null)
const records = ref([])
const timeRange = ref([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})
const queryForm = reactive({
  userId: undefined,
  module: '',
  action: ''
})

async function loadList() {
  loading.value = true

  try {
    const result = await operationLogApi.page({
      page: pagination.page,
      size: pagination.size,
      userId: queryForm.userId,
      module: queryForm.module,
      action: queryForm.action,
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
  queryForm.userId = undefined
  queryForm.module = ''
  queryForm.action = ''
  timeRange.value = []
  handleSearch()
}

function handleSizeChange() {
  pagination.page = 1
  loadList()
}

async function openDetail(row) {
  currentLog.value = await operationLogApi.detail(row.id)
  detailVisible.value = true
}

onMounted(loadList)
</script>
