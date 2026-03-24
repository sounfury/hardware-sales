<script setup>
import { getInventoryLogPage } from '@/api/inventory'
import { getProductPage } from '@/api/product'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const productOptions = ref([])

const searchForm = reactive({
  productId: null,
  type: null,
  dateRange: [],
})

const typeOptions = [
  { label: '入库', value: 1, type: 'success' },
  { label: '出库', value: 2, type: 'warning' },
]

function getTypeMeta(type) {
  return typeOptions.find((item) => item.value === type) || { label: '未知', type: 'info' }
}

function getRefLabel(refType) {
  if (refType === 'PURCHASE') return '采购'
  if (refType === 'SALES') return '销售'
  return refType || '-'
}

async function loadProducts() {
  const res = await getProductPage({
    pageNum: 1,
    pageSize: 200,
  })
  productOptions.value = res.data.records || []
}

async function loadList() {
  loading.value = true
  try {
    const res = await getInventoryLogPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      productId: searchForm.productId || undefined,
      type: searchForm.type ?? undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  searchForm.productId = null
  searchForm.type = null
  searchForm.dateRange = []
  handleSearch()
}

onMounted(async () => {
  await loadProducts()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <div>
      <h2 class="text-lg font-semibold text-gray-800">库存流水</h2>
      <p class="mt-0.5 text-sm text-gray-400">查询商品的入库、出库变化和关联业务单据</p>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
        <el-select
          v-model="searchForm.productId"
          placeholder="商品"
          clearable
          filterable
          class="!w-56"
        >
          <el-option
            v-for="product in productOptions"
            :key="product.id"
            :label="product.name"
            :value="product.id"
          />
        </el-select>
        <el-select v-model="searchForm.type" placeholder="出入库类型" clearable class="!w-40">
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon class="mr-1"><Refresh /></el-icon>重置
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeMeta(row.type).type" effect="plain">
              {{ getTypeMeta(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="变动数量" width="100" align="center" />
        <el-table-column prop="beforeStock" label="变动前库存" width="110" align="center" />
        <el-table-column prop="afterStock" label="变动后库存" width="110" align="center" />
        <el-table-column label="关联业务" width="100" align="center">
          <template #default="{ row }">{{ getRefLabel(row.refType) }}</template>
        </el-table-column>
        <el-table-column prop="refOrderId" label="关联单据ID" width="120" align="center" />
        <el-table-column label="记录时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end border-t border-gray-100 pt-4">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>
