<script setup>
import { ElMessage } from 'element-plus'
import {
  getSupplierProductPage,
  addSupplierProduct,
  updateSupplierProduct,
  deleteSupplierProduct,
} from '@/api/supplierProduct'
import { getProductPage } from '@/api/product'
import { getSupplierPage } from '@/api/supplier'
import { formatDateTime, formatMoney } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  supplierId: null,
  productId: null,
})

const supplierOptions = ref([])
const supplierMap = computed(() =>
  supplierOptions.value.reduce((acc, item) => {
    acc[item.id] = item.companyName
    return acc
  }, {}),
)
const productOptions = ref([])
const productMap = computed(() =>
  productOptions.value.reduce((acc, item) => {
    acc[item.id] = item
    return acc
  }, {}),
)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  supplierId: null,
  productId: null,
  supplyPrice: null,
  remark: '',
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  supplyPrice: [{ required: true, message: '请输入供货价格', trigger: 'blur' }],
}

async function loadSuppliers() {
  const res = await getSupplierPage({
    pageNum: 1,
    pageSize: 200,
  })
  supplierOptions.value = res.data.records || []
}

async function loadProducts() {
  const res = await getProductPage({
    pageNum: 1,
    pageSize: 500,
  })
  productOptions.value = res.data.records || []
}

async function loadList() {
  loading.value = true
  try {
    const res = await getSupplierProductPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      supplierId: searchForm.supplierId || undefined,
      productId: searchForm.productId || undefined,
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
  searchForm.supplierId = null
  searchForm.productId = null
  handleSearch()
}

function resetForm() {
  formData.id = null
  formData.supplierId = null
  formData.productId = null
  formData.supplyPrice = null
  formData.remark = ''
}

function handleAdd() {
  dialogTitle.value = '新增供应商商品'
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEdit(row) {
  dialogTitle.value = '编辑供应商商品'
  Object.assign(formData, {
    id: row.id,
    supplierId: row.supplierId,
    productId: row.productId,
    supplyPrice: row.supplyPrice,
    remark: row.remark,
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = { ...formData }
  if (payload.id) {
    await updateSupplierProduct(payload)
    ElMessage.success('供应商商品更新成功')
  } else {
    await addSupplierProduct(payload)
    ElMessage.success('供应商商品创建成功')
  }
  dialogVisible.value = false
  loadList()
}

async function handleDelete(id) {
  await deleteSupplierProduct(id)
  ElMessage.success('供应商商品删除成功')
  loadList()
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
        <h2 class="text-lg font-semibold text-gray-800">供应商商品</h2>
        <p class="mt-0.5 text-sm text-gray-400">管理供应商可供货商品、规格和报价</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon class="mr-1"><Plus /></el-icon>新增供货商品
      </el-button>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
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
        <el-select
          v-model="searchForm.productId"
          placeholder="商品"
          clearable
          class="!w-56"
          filterable
        >
          <el-option
            v-for="item in productOptions"
            :key="item.id"
            :label="`${item.name}${item.spec ? `（${item.spec}）` : ''}`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon class="mr-1"><Refresh /></el-icon>重置
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="供应商" min-width="180">
          <template #default="{ row }">{{ row.supplierName || supplierMap[row.supplierId] || `#${row.supplierId}` }}</template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="productSpec" label="规格型号" min-width="160" show-overflow-tooltip />
        <el-table-column label="单位" width="80" align="center">
          <template #default="{ row }">{{ row.productUnit || productMap[row.productId]?.unit || '-' }}</template>
        </el-table-column>
        <el-table-column label="供货价" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.supplyPrice) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该供货商品？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
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
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px" class="pr-4">
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="formData.supplierId" placeholder="请选择供应商" class="!w-full" filterable>
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="系统商品" prop="productId">
          <el-select v-model="formData.productId" placeholder="请选择商品" class="!w-full" filterable>
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="`${item.name}${item.spec ? `（${item.spec}）` : ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="供货价格" prop="supplyPrice">
          <el-input-number
            v-model="formData.supplyPrice"
            :min="0"
            :precision="2"
            controls-position="right"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
