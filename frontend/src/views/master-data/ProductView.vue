<template>
  <MasterCrudPage
    ref="masterPageRef"
    title="服装商品"
    description="维护 SKU、分类、尺码、颜色、季节、价格和低库存阈值。"
    :api="productApi"
    :filters="filters"
    :columns="columns"
    :fields="fields"
    :default-form="{ status: 'ACTIVE', lowStockThreshold: 0, costPrice: 0, salePrice: 0 }"
  >
    <template #header-actions>
      <ElButton @click="openCompanyImport">
        <ElIcon><Upload /></ElIcon>
        导入公司商品表
      </ElButton>
    </template>
  </MasterCrudPage>

  <ElDialog
    v-model="companyImportVisible"
    title="导入公司服装商品"
    width="640px"
    destroy-on-close
  >
    <ElForm label-position="top">
      <ElFormItem label="商品分类" required>
        <ElSelect
          v-model="companyImportCategoryId"
          filterable
          placeholder="请选择本次导入商品所属分类"
          style="width: 100%"
        >
          <ElOption
            v-for="option in categoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
      </ElFormItem>
    </ElForm>

    <ElUpload
      ref="companyUploadRef"
      drag
      :auto-upload="false"
      :limit="1"
      accept=".xls,.xlsx"
      :on-change="handleCompanyFileChange"
      :on-remove="handleCompanyFileRemove"
    >
      <ElIcon class="el-icon--upload"><Upload /></ElIcon>
      <div class="el-upload__text">拖拽公司库存统计表或校服库存数量表到此处，或点击选择</div>
      <template #tip>
        <div class="el-upload__tip">支持公司数量统计表和校服库存数量表。</div>
      </template>
    </ElUpload>

    <ElAlert
      class="company-import-notice"
      type="warning"
      show-icon
      :closable="false"
      title="导入会同时创建商品并初始化库存"
    >
      <template #default>
        <ul class="company-import-rules">
          <li>公司数量表按型号、颜色和尺码创建；校服库存表按名称、标题和尺码创建。</li>
          <li>SKU 自动生成为“型号-颜色-尺码”；已有一致 SKU 复用商品。</li>
          <li>库存写入唯一启用仓库并生成流水，任一错误将整表回滚。</li>
        </ul>
      </template>
    </ElAlert>

    <div v-if="companyImportResult" class="company-import-result">
      <ElAlert title="导入完成" type="success" show-icon :closable="false" />
      <ElDescriptions :column="2" border>
        <ElDescriptionsItem label="导入批次">{{ companyImportResult.batchNo }}</ElDescriptionsItem>
        <ElDescriptionsItem label="分类">{{ companyImportResult.categoryName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="仓库">{{ companyImportResult.warehouseName }}</ElDescriptionsItem>
        <ElDescriptionsItem label="规格总数">{{ companyImportResult.specificationCount }}</ElDescriptionsItem>
        <ElDescriptionsItem label="新建商品">{{ companyImportResult.createdProductCount }}</ElDescriptionsItem>
        <ElDescriptionsItem label="复用商品">{{ companyImportResult.reusedProductCount }}</ElDescriptionsItem>
        <ElDescriptionsItem label="新建库存">{{ companyImportResult.createdStockCount }}</ElDescriptionsItem>
        <ElDescriptionsItem label="更新库存">{{ companyImportResult.updatedStockCount }}</ElDescriptionsItem>
        <ElDescriptionsItem label="库存未变化">{{ companyImportResult.unchangedStockCount }}</ElDescriptionsItem>
        <ElDescriptionsItem label="零库存规格">{{ companyImportResult.zeroStockCount }}</ElDescriptionsItem>
      </ElDescriptions>
    </div>

    <template #footer>
      <ElButton @click="companyImportVisible = false">关闭</ElButton>
      <ElButton
        type="primary"
        :loading="companyImporting"
        :disabled="!companyImportCategoryId || !companyImportFile"
        @click="handleCompanyImport"
      >
        开始导入
      </ElButton>
    </template>
  </ElDialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'

import { categoryApi, productApi } from '@/api/business'
import { masterStatusOptions } from '@/constants/options'
import { formatDateTime, formatMoney } from '@/utils/format'
import MasterCrudPage from './MasterCrudPage.vue'

const categories = ref([])
const masterPageRef = ref(null)
const companyUploadRef = ref(null)
const companyImportVisible = ref(false)
const companyImporting = ref(false)
const companyImportCategoryId = ref('')
const companyImportFile = ref(null)
const companyImportResult = ref(null)

const categoryOptions = computed(() =>
  categories.value.map((category) => ({
    label: `${category.code} - ${category.name}`,
    value: category.id
  }))
)

function categoryLabel(categoryId) {
  const category = categories.value.find((item) => item.id === categoryId)

  return category ? category.name : categoryId || '-'
}

const filters = computed(() => [
  {
    prop: 'keyword',
    label: '关键词',
    type: 'input',
    keyword: true,
    placeholder: 'SKU / 名称'
  },
  {
    prop: 'categoryId',
    label: '分类',
    type: 'select',
    options: categoryOptions.value
  },
  {
    prop: 'brand',
    label: '品牌',
    type: 'input'
  },
  {
    prop: 'season',
    label: '季节',
    type: 'input'
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  }
])

const columns = computed(() => [
  {
    prop: 'sku',
    label: 'SKU',
    minWidth: 140
  },
  {
    prop: 'name',
    label: '商品名称',
    minWidth: 180
  },
  {
    prop: 'categoryId',
    label: '分类',
    minWidth: 130,
    formatter: categoryLabel
  },
  {
    prop: 'size',
    label: '尺码',
    width: 90
  },
  {
    prop: 'color',
    label: '颜色',
    width: 100
  },
  {
    prop: 'brand',
    label: '品牌',
    minWidth: 120
  },
  {
    prop: 'costPrice',
    label: '成本价',
    width: 110,
    formatter: formatMoney
  },
  {
    prop: 'salePrice',
    label: '销售价',
    width: 110,
    formatter: formatMoney
  },
  {
    prop: 'lowStockThreshold',
    label: '预警阈值',
    width: 110
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    statusOptions: masterStatusOptions
  },
  {
    prop: 'updatedAt',
    label: '更新时间',
    minWidth: 170,
    formatter: formatDateTime
  }
])

const fields = computed(() => [
  {
    prop: 'sku',
    label: 'SKU',
    required: true,
    maxlength: 80
  },
  {
    prop: 'name',
    label: '商品名称',
    required: true,
    maxlength: 150
  },
  {
    prop: 'categoryId',
    label: '分类',
    type: 'select',
    required: true,
    options: categoryOptions.value
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    options: masterStatusOptions
  },
  {
    prop: 'size',
    label: '尺码',
    maxlength: 30
  },
  {
    prop: 'color',
    label: '颜色',
    maxlength: 50
  },
  {
    prop: 'brand',
    label: '品牌',
    maxlength: 100
  },
  {
    prop: 'season',
    label: '季节',
    maxlength: 50
  },
  {
    prop: 'costPrice',
    label: '成本价',
    type: 'number',
    min: 0,
    precision: 2,
    step: 1
  },
  {
    prop: 'salePrice',
    label: '销售价',
    type: 'number',
    min: 0,
    precision: 2,
    step: 1
  },
  {
    prop: 'lowStockThreshold',
    label: '预警阈值',
    type: 'number',
    min: 0
  }
])

function openCompanyImport() {
  companyImportCategoryId.value = ''
  companyImportFile.value = null
  companyImportResult.value = null
  companyImportVisible.value = true
}

function handleCompanyFileChange(uploadFile) {
  companyImportFile.value = uploadFile.raw || null
  companyImportResult.value = null
}

function handleCompanyFileRemove() {
  companyImportFile.value = null
}

async function handleCompanyImport() {
  if (!companyImportCategoryId.value) {
    ElMessage.warning('请选择商品分类')
    return
  }
  if (!companyImportFile.value) {
    ElMessage.warning('请选择公司库存统计表')
    return
  }

  try {
    await ElMessageBox.confirm(
      '系统将创建全部尺码商品规格，并按表格数量初始化或覆盖唯一仓库库存。确认继续吗？',
      '确认导入公司商品',
      {
        type: 'warning',
        confirmButtonText: '确认导入',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }

  companyImporting.value = true
  try {
    const result = await productApi.importCompanyStock(
      companyImportCategoryId.value,
      companyImportFile.value
    )
    companyImportFile.value = null
    companyUploadRef.value?.clearFiles()
    companyImportResult.value = result
    ElMessage.success('公司商品和库存导入成功')
    await masterPageRef.value?.refresh()
  } finally {
    companyImporting.value = false
  }
}

onMounted(async () => {
  const result = await categoryApi.page({
    page: 1,
    size: 200,
    status: 'ACTIVE'
  })

  categories.value = result.records || []
})
</script>

<style scoped>
.company-import-notice {
  margin-top: 18px;
}

.company-import-rules {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.company-import-result {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}
</style>
