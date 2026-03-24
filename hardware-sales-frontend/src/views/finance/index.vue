<script setup>
import {
  getFinancePage,
  getFinanceSummary,
  getReceivableAmount,
  getPayableAmount,
} from '@/api/finance'
import { formatDateTime, formatMoney } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const summary = reactive({
  totalIncome: 0,
  totalExpense: 0,
  profit: 0,
  receivable: 0,
  payable: 0,
})

const searchForm = reactive({
  type: null,
  paymentStatus: null,
  dateRange: [],
})

const typeMap = {
  1: { label: '收入', type: 'success' },
  2: { label: '支出', type: 'danger' },
}

const statusMap = {
  0: { label: '未结算', type: 'warning' },
  1: { label: '已结算', type: 'success' },
}

function getTypeMeta(type) {
  return typeMap[type] || { label: '未知', type: 'info' }
}

function getStatusMeta(status) {
  return statusMap[status] || { label: '未知', type: 'info' }
}

function getRefLabel(refType) {
  if (refType === 'PURCHASE') return '采购单'
  if (refType === 'SALES') return '销售单'
  return refType || '-'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getFinancePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: searchForm.type ?? undefined,
      paymentStatus: searchForm.paymentStatus ?? undefined,
      startDate: searchForm.dateRange?.[0] || undefined,
      endDate: searchForm.dateRange?.[1] || undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  const [summaryRes, receivableRes, payableRes] = await Promise.all([
    getFinanceSummary(),
    getReceivableAmount(),
    getPayableAmount(),
  ])
  Object.assign(summary, summaryRes.data)
  summary.receivable = receivableRes.data
  summary.payable = payableRes.data
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  searchForm.type = null
  searchForm.paymentStatus = null
  searchForm.dateRange = []
  handleSearch()
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadList()])
})
</script>

<template>
  <div class="space-y-4">
    <div class="grid gap-4 md:grid-cols-5">
      <el-card v-for="card in [
        { label: '总收入', value: summary.totalIncome, color: 'text-emerald-600' },
        { label: '总支出', value: summary.totalExpense, color: 'text-rose-500' },
        { label: '净利润', value: summary.profit, color: 'text-primary' },
        { label: '应收金额', value: summary.receivable, color: 'text-amber-500' },
        { label: '应付金额', value: summary.payable, color: 'text-indigo-500' },
      ]" :key="card.label" shadow="never" class="!border-gray-200/80">
        <p class="text-sm text-gray-400">{{ card.label }}</p>
        <p class="mt-3 text-2xl font-semibold" :class="card.color">{{ formatMoney(card.value) }}</p>
      </el-card>
    </div>

    <div>
      <h2 class="text-lg font-semibold text-gray-800">财务管理</h2>
      <p class="mt-0.5 text-sm text-gray-400">查看收入支出记录、结算状态以及整体收支汇总</p>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
        <el-select v-model="searchForm.type" placeholder="收支类型" clearable class="!w-40">
          <el-option label="收入" :value="1" />
          <el-option label="支出" :value="2" />
        </el-select>
        <el-select v-model="searchForm.paymentStatus" placeholder="结算状态" clearable class="!w-40">
          <el-option label="未结算" :value="0" />
          <el-option label="已结算" :value="1" />
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
        <el-table-column prop="id" label="记录ID" width="90" align="center" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeMeta(row.type).type" effect="plain">
              {{ getTypeMeta(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="关联业务" width="100" align="center">
          <template #default="{ row }">{{ getRefLabel(row.refType) }}</template>
        </el-table-column>
        <el-table-column prop="refOrderId" label="关联单据ID" width="120" align="center" />
        <el-table-column label="结算状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.paymentStatus).type" effect="plain">
              {{ getStatusMeta(row.paymentStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
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
