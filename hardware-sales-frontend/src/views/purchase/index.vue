<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPurchasePage,
  getPurchaseDetail,
  createPurchaseOrder,
  settlePurchaseOrder,
} from '@/api/purchase'
import { getSupplierProductList } from '@/api/supplierProduct'
import { getSupplierPage } from '@/api/supplier'
import { getProductPage } from '@/api/product'
import { formatDate, formatDateTime, formatMoney } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const supplierOptions = ref([])
const productOptions = ref([])
const supplierProductOptions = ref([])
const supplierMap = computed(() =>
  supplierOptions.value.reduce((acc, item) => {
    acc[item.id] = item.companyName
    return acc
  }, {}),
)
const productMap = computed(() =>
  productOptions.value.reduce((acc, item) => {
    acc[item.id] = item
    return acc
  }, {}),
)
const supplierProductMap = computed(() =>
  supplierProductOptions.value.reduce((acc, item) => {
    acc[item.productId] = item
    return acc
  }, {}),
)

const searchForm = reactive({
  orderNo: '',
  supplierId: null,
  dateRange: [],
})

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  supplierId: null,
  orderDate: '',
  remark: '',
  items: [],
})

const createRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
}

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

function createEmptyItem() {
  return {
    productId: null,
    quantity: 1,
    price: null,
  }
}

const createTotal = computed(() =>
  createForm.items.reduce((sum, item) => {
    const quantity = Number(item.quantity || 0)
    const price = Number(item.price || 0)
    return sum + quantity * price
  }, 0),
)

const paymentStatusMap = {
  0: { label: '未结算', type: 'warning' },
  1: { label: '已结算', type: 'success' },
}

function getPaymentMeta(status) {
  return paymentStatusMap[status] || { label: '未知', type: 'info' }
}

async function loadSuppliers() {
  const res = await getSupplierPage({
    pageNum: 1,
    pageSize: 200,
    auditStatus: 1,
  })
  supplierOptions.value = res.data.records || []
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
    const res = await getPurchasePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderNo: searchForm.orderNo || undefined,
      supplierId: searchForm.supplierId || undefined,
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
  searchForm.orderNo = ''
  searchForm.supplierId = null
  searchForm.dateRange = []
  handleSearch()
}

function resetCreateForm() {
  createForm.supplierId = null
  createForm.orderDate = ''
  createForm.remark = ''
  createForm.items = [createEmptyItem()]
  supplierProductOptions.value = []
}

function handleAdd() {
  resetCreateForm()
  createDialogVisible.value = true
  nextTick(() => createFormRef.value?.clearValidate())
}

function addItem() {
  if (!createForm.supplierId) {
    ElMessage.warning('请先选择供应商')
    return
  }
  createForm.items.push(createEmptyItem())
}

function removeItem(index) {
  if (createForm.items.length === 1) return
  createForm.items.splice(index, 1)
}

async function loadSupplierProducts(supplierId) {
  if (!supplierId) {
    supplierProductOptions.value = []
    return
  }
  const res = await getSupplierProductList(supplierId)
  supplierProductOptions.value = res.data || []
}

/**
 * 切换供应商后必须重置采购明细，避免旧供应商的商品和价格残留到新采购单里。
 */
async function handleSupplierChange() {
  createForm.items = [createEmptyItem()]
  await loadSupplierProducts(createForm.supplierId)
  if (createForm.supplierId && !supplierProductOptions.value.length) {
    ElMessage.warning('该供应商暂无供货商品，请先通知供应商在微信小程序维护报价')
  }
}

function handleProductChange(item) {
  item.price = supplierProductMap.value[item.productId]?.supplyPrice ?? null
}

function getLineAmount(item) {
  return Number(item.quantity || 0) * Number(item.price || 0)
}

function getSupplierProductLabel(item) {
  const product = productMap.value[item.productId]
  const specLabel = item.productSpec ? ` ${item.productSpec}` : ''
  const stockLabel = product ? `，库存 ${product.stock}` : ''
  return `${item.productName}${specLabel}，供货价 ${formatMoney(item.supplyPrice)}${stockLabel}`
}

async function handleCreateSubmit() {
  await createFormRef.value.validate()
  const invalidItem = createForm.items.find(
    (item) => !item.productId || !item.quantity || item.price == null,
  )
  if (invalidItem) {
    ElMessage.warning('请完整填写采购明细')
    return
  }
  await createPurchaseOrder({
    supplierId: createForm.supplierId,
    orderDate: createForm.orderDate,
    remark: createForm.remark,
    items: createForm.items.map((item) => ({
      productId: item.productId,
      quantity: Number(item.quantity),
    })),
  })
  ElMessage.success('采购单创建成功')
  createDialogVisible.value = false
  loadList()
}

async function handleViewDetail(id) {
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    const res = await getPurchaseDetail(id)
    detailData.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function handleSettle(row) {
  await ElMessageBox.confirm(`确认结算采购单 ${row.orderNo}？`, '结算确认', { type: 'warning' })
  await settlePurchaseOrder(row.id)
  ElMessage.success('采购单已结算')
  loadList()
  if (detailData.value?.id === row.id) {
    handleViewDetail(row.id)
  }
}

onMounted(async () => {
  await Promise.all([loadSuppliers(), loadProducts()])
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-800">采购管理</h2>
        <p class="mt-0.5 text-sm text-gray-400">创建采购单、查看明细并跟踪付款结算状态</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon class="mr-1"><Plus /></el-icon>新建采购单
      </el-button>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
        <el-input
          v-model="searchForm.orderNo"
          placeholder="采购单号"
          clearable
          class="!w-48"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.supplierId"
          placeholder="供应商"
          clearable
          filterable
          class="!w-56"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id"
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
          <el-icon class="mr-1"><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon class="mr-1"><Refresh /></el-icon>重置
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="采购单号" min-width="200" />
        <el-table-column prop="supplierName" label="供应商" min-width="180" />
        <el-table-column label="总金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="付款状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getPaymentMeta(row.paymentStatus).type" effect="plain">
              {{ getPaymentMeta(row.paymentStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="采购日期" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleViewDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.paymentStatus === 0"
              type="success"
              text
              size="small"
              @click="handleSettle(row)"
            >
              结算
            </el-button>
            <span v-else class="text-xs text-gray-400">已结算</span>
          </template>
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

    <el-dialog
      v-model="createDialogVisible"
      title="新建采购单"
      width="920"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="88px" class="pr-4">
        <div class="grid gap-4 md:grid-cols-2">
          <el-form-item label="供应商" prop="supplierId">
            <el-select
              v-model="createForm.supplierId"
              placeholder="请选择供应商"
              class="!w-full"
              filterable
              @change="handleSupplierChange"
            >
              <el-option
                v-for="item in supplierOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="采购日期" prop="orderDate">
            <el-date-picker
              v-model="createForm.orderDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择采购日期"
              class="!w-full"
            />
          </el-form-item>
        </div>
        <el-form-item label="采购备注">
          <el-input v-model="createForm.remark" placeholder="请输入采购备注" maxlength="255" />
        </el-form-item>

        <div class="rounded-xl border border-gray-200 bg-gray-50/70 p-4">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <p class="font-medium text-gray-800">采购明细</p>
              <p class="text-xs text-gray-400">先选供应商，再从其供货商品中选货；供货价统一按供应商商品报价带出</p>
            </div>
            <el-button type="primary" plain @click="addItem">
              <el-icon class="mr-1"><Plus /></el-icon>添加明细
            </el-button>
          </div>

          <div v-for="(item, index) in createForm.items" :key="index" class="mb-3 rounded-lg bg-white p-3 shadow-sm last:mb-0">
            <div class="grid gap-3 md:grid-cols-[2fr_1fr_1fr_auto]">
              <el-select
                v-model="item.productId"
                placeholder="选择商品"
                filterable
                :disabled="!createForm.supplierId"
                @change="handleProductChange(item)"
              >
                <el-option
                  v-for="product in supplierProductOptions"
                  :key="product.id"
                  :label="getSupplierProductLabel(product)"
                  :value="product.productId"
                />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" controls-position="right" class="!w-full" />
              <el-input-number
                v-model="item.price"
                :min="0"
                :precision="2"
                controls-position="right"
                class="!w-full"
                disabled
              />
              <el-button text type="danger" @click="removeItem(index)">删除</el-button>
            </div>
            <div class="mt-2 text-right text-sm text-gray-500">
              小计：<span class="font-medium text-gray-800">{{ formatMoney(getLineAmount(item)) }}</span>
            </div>
          </div>
        </div>

        <div class="mt-4 flex justify-end">
          <div class="rounded-xl bg-primary-light px-4 py-3 text-right">
            <p class="text-xs text-gray-500">采购总金额</p>
            <p class="text-2xl font-semibold text-primary">{{ formatMoney(createTotal) }}</p>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateSubmit">提交采购单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="采购单详情" width="880" destroy-on-close>
      <div v-loading="detailLoading" class="space-y-4">
        <template v-if="detailData">
          <div class="grid gap-4 rounded-xl bg-gray-50 p-4 md:grid-cols-4">
            <div>
              <p class="text-xs text-gray-400">采购单号</p>
              <p class="mt-1 font-medium text-gray-800">{{ detailData.orderNo }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-400">供应商</p>
              <p class="mt-1 font-medium text-gray-800">{{ detailData.supplierName || supplierMap[detailData.supplierId] || '-' }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-400">采购日期</p>
              <p class="mt-1 font-medium text-gray-800">{{ formatDate(detailData.orderDate) }}</p>
            </div>
            <div>
              <p class="text-xs text-gray-400">付款状态</p>
              <el-tag class="mt-1" :type="getPaymentMeta(detailData.paymentStatus).type" effect="plain">
                {{ getPaymentMeta(detailData.paymentStatus).label }}
              </el-tag>
            </div>
          </div>

          <el-table :data="detailData.items || []" stripe>
            <el-table-column prop="productName" label="商品名称" min-width="180" />
            <el-table-column prop="quantity" label="数量" width="90" align="center" />
            <el-table-column label="单价" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>

          <div class="flex items-center justify-between rounded-xl border border-gray-200 px-4 py-3">
            <div class="text-sm text-gray-500">备注：{{ detailData.remark || '无' }}</div>
            <div class="text-right">
              <p class="text-xs text-gray-400">采购总金额</p>
              <p class="text-xl font-semibold text-gray-800">{{ formatMoney(detailData.totalAmount) }}</p>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>
