<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductPage, addProduct, updateProduct, deleteProduct, completeRestock } from '@/api/product'
import { getCategoryList } from '@/api/category'
import { getSupplierProductByProduct } from '@/api/supplierProduct'
import { sendRestockMessage } from '@/api/message'

/** 分类数据（搜索栏 + 表单下拉共用） */
const categoryList = ref([])
const selectableCategoryList = ref([])
const categoryMap = ref({})

const formCategoryOptions = computed(() => {
  const optionMap = new Map(
    selectableCategoryList.value.map((item) => [item.id, { ...item, label: item.name, disabled: false }]),
  )
  const currentCategory = categoryList.value.find((item) => item.id === formData.categoryId)
  if (currentCategory && !optionMap.has(currentCategory.id)) {
    optionMap.set(currentCategory.id, {
      ...currentCategory,
      label: `${currentCategory.name}（已停用）`,
      disabled: true,
    })
  }
  return Array.from(optionMap.values())
})

async function loadCategories() {
  const [allRes, activeRes] = await Promise.all([
    getCategoryList(),
    getCategoryList({ status: 1 }),
  ])
  categoryList.value = allRes.data
  selectableCategoryList.value = activeRes.data
  categoryMap.value = {}
  allRes.data.forEach((c) => {
    categoryMap.value[c.id] = Number(c.status) === 1 ? c.name : `${c.name}（已停用）`
  })
}

/** 搜索条件 */
const searchForm = reactive({
  name: '',
  brand: '',
  categoryId: null,
})

/** 分页与表格 */
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const quoteDialogVisible = ref(false)
const quoteLoading = ref(false)
const quoteList = ref([])
const currentProduct = ref(null)
const restockContent = ref('当前商品库存偏低，请尽快安排补货。')

/** 加载商品列表 */
async function loadList() {
  loading.value = true
  try {
    const res = await getProductPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      name: searchForm.name || undefined,
      brand: searchForm.brand || undefined,
      categoryId: searchForm.categoryId || undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pageNum.value = 1
  loadList()
}

/** 重置搜索 */
function handleReset() {
  searchForm.name = ''
  searchForm.brand = ''
  searchForm.categoryId = null
  handleSearch()
}

/** 分页切换 */
function handleSizeChange(val) {
  pageSize.value = val
  pageNum.value = 1
  loadList()
}

function handleCurrentChange(val) {
  pageNum.value = val
  loadList()
}

/** 对话框状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  categoryId: null,
  name: '',
  brand: '',
  spec: '',
  description: '',
  unit: '',
  purchasePrice: null,
  salePrice: null,
  stock: 0,
  restockThreshold: 10,
})
const formRules = {
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }],
  purchasePrice: [{ required: true, message: '请输入采购价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }],
  restockThreshold: [{ required: true, message: '请输入补货阈值', trigger: 'blur' }],
}

/** 重置表单数据 */
function resetForm() {
  formData.id = null
  formData.categoryId = null
  formData.name = ''
  formData.brand = ''
  formData.spec = ''
  formData.description = ''
  formData.unit = ''
  formData.purchasePrice = null
  formData.salePrice = null
  formData.stock = 0
  formData.restockThreshold = 10
}

/** 打开新增对话框 */
function handleAdd() {
  dialogTitle.value = '新增商品'
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

/** 打开编辑对话框 */
function handleEdit(row) {
  dialogTitle.value = '编辑商品'
  Object.assign(formData, {
    id: row.id,
    categoryId: row.categoryId,
    name: row.name,
    brand: row.brand,
    spec: row.spec,
    description: row.description,
    unit: row.unit,
    purchasePrice: row.purchasePrice,
    salePrice: row.salePrice,
    stock: row.stock,
    restockThreshold: row.restockThreshold ?? 10,
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

/** 提交表单 */
async function handleSubmit() {
  await formRef.value.validate()
  if (formData.id) {
    await updateProduct({ ...formData })
    ElMessage.success('修改成功')
  } else {
    await addProduct({ ...formData })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadList()
}

/** 删除商品 */
async function handleDelete(id) {
  await deleteProduct(id)
  ElMessage.success('删除成功')
  loadList()
}

/**
 * 打开供应商报价弹窗，并加载当前商品的可供货供应商。
 */
async function openSupplierDialog(product) {
  quoteDialogVisible.value = true
  currentProduct.value = product
  restockContent.value = `商品【${product.name}】当前库存为 ${product.stock}，请尽快安排补货。`
  quoteLoading.value = true
  try {
    const res = await getSupplierProductByProduct(product.id)
    quoteList.value = res.data || []
  } finally {
    quoteLoading.value = false
  }
}

/**
 * 向选中的供应商发送补货提醒。
 */
async function handleSendRestock(supplierProduct) {
  if (!currentProduct.value) return
  await sendRestockMessage({
    receiverId: supplierProduct.supplierUserId,
    productId: currentProduct.value.id,
    content: restockContent.value,
  })
  ElMessage.success('补货提醒已发送')
  quoteDialogVisible.value = false
  await loadList()
}

/**
 * 管理员确认供应商已完成补货后，手动结束当前补货状态。
 */
async function handleCompleteRestock(product) {
  await ElMessageBox.confirm(`确认将商品【${product.name}】标记为补货完成？`, '提示', {
    type: 'warning',
  })
  await completeRestock(product.id)
  ElMessage.success('补货状态已恢复为正常')
  quoteDialogVisible.value = false
  await loadList()
}

/** 格式化金额 */
function formatPrice(val) {
  if (val == null) return '-'
  return `¥${Number(val).toFixed(2)}`
}

function getRestockThreshold(product) {
  return Number(product?.restockThreshold ?? 10)
}

function isLowStock(product) {
  if (!product) return false
  return Number(product?.stock ?? 0) <= getRestockThreshold(product)
}

function isRestocking(product) {
  if (!product) return false
  return Number(product?.restockStatus ?? 0) === 1
}

function getRestockStatusMeta(product) {
  if (isRestocking(product)) return { label: '补货中', type: 'warning' }
  if (isLowStock(product)) return { label: '低库存', type: 'danger' }
  return { label: '正常', type: 'success' }
}

onMounted(() => {
  loadCategories()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 页头 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-gray-800">商品管理</h2>
        <p class="text-sm text-gray-400 mt-0.5">管理商品基础信息、补货阈值与当前补货状态</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon class="mr-1"><Plus /></el-icon>新增商品
      </el-button>
    </div>

    <!-- 搜索栏 + 表格卡片 -->
    <el-card shadow="never" class="!border-gray-200/80">
      <!-- 搜索栏 -->
      <div class="flex flex-wrap items-center gap-3 mb-4 pb-4 border-b border-gray-100">
        <el-input
          v-model="searchForm.name"
          placeholder="商品名称"
          clearable
          class="!w-48"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="searchForm.brand"
          placeholder="品牌"
          clearable
          class="!w-36"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.categoryId"
          placeholder="商品分类"
          clearable
          class="!w-40"
        >
          <el-option
            v-for="c in categoryList"
            :key="c.id"
            :label="categoryMap[c.id] || c.name"
            :value="c.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon>搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon class="mr-1"><Refresh /></el-icon>重置
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="商品名称" min-width="160">
          <template #default="{ row }">
            <span class="font-medium text-gray-700">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="120" />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ categoryMap[row.categoryId] || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格型号" width="140" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="采购价" width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-sm">{{ formatPrice(row.purchasePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售价" width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-sm text-primary">{{ formatPrice(row.salePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90" align="center">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono text-sm font-medium',
                isLowStock(row) ? 'text-red-500' : 'text-gray-600',
              ]"
            >
              {{ row.stock }}
            </span>
            <el-icon v-if="isLowStock(row)" class="ml-0.5 text-red-400 text-xs"><WarningFilled /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="restockThreshold" label="补货阈值" width="100" align="center" />
        <el-table-column label="补货状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRestockStatusMeta(row).type" effect="plain">
              {{ getRestockStatusMeta(row).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <!-- 情境化主操作 -->
              <!-- 1. 补货中 -> 完成补货 -->
              <el-button
                v-if="isRestocking(row)"
                type="success"
                link
                size="small"
                @click="handleCompleteRestock(row)"
              >
                <el-icon class="mr-0.5"><Check /></el-icon>完成补货
              </el-button>
              <!-- 2. 低库存 -> 提醒补货 -->
              <el-button
                v-else-if="isLowStock(row)"
                type="warning"
                link
                size="small"
                @click="openSupplierDialog(row)"
              >
                <el-icon class="mr-0.5"><Bell /></el-icon>提醒补货
              </el-button>
              <!-- 3. 正常状态 -> 编辑商品 -->
              <el-button
                v-else
                type="primary"
                link
                size="small"
                @click="handleEdit(row)"
              >
                <el-icon class="mr-0.5"><Edit /></el-icon>编辑商品
              </el-button>

              <div class="w-px h-3 bg-slate-200"></div>

              <!-- 更多操作：根据主操作动态调整下拉内容 -->
              <el-dropdown trigger="click">
                <el-button link size="small" type="primary">
                  更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <!-- 如果主操作不是编辑，就把编辑放进这里 -->
                    <el-dropdown-item v-if="isLowStock(row) || isRestocking(row)" @click="handleEdit(row)">
                      <el-icon><Edit /></el-icon>编辑信息
                    </el-dropdown-item>
                    <el-dropdown-item @click="openSupplierDialog(row)">
                      <el-icon><View /></el-icon>查看报价
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDelete(row.id)" class="!text-rose-500">
                      <el-icon><Delete /></el-icon>删除商品
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="flex justify-end mt-4 pt-4 border-t border-gray-100">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
        label-position="right"
        class="pr-4"
      >
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" class="!w-full">
            <el-option
              v-for="c in formCategoryOptions"
              :key="c.id"
              :label="c.label"
              :value="c.id"
              :disabled="c.disabled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入商品名称" maxlength="50" clearable />
        </el-form-item>
        <div class="flex gap-4">
          <el-form-item label="品牌" class="flex-1">
            <el-input v-model="formData.brand" placeholder="请输入品牌" clearable />
          </el-form-item>
          <el-form-item label="单位" prop="unit" class="!w-36" label-width="50px">
            <el-input v-model="formData.unit" placeholder="如：台、把" clearable />
          </el-form-item>
        </div>
        <el-form-item label="规格型号">
          <el-input v-model="formData.spec" placeholder="请输入规格型号" clearable />
        </el-form-item>
        <div class="flex gap-4">
          <el-form-item label="采购价" prop="purchasePrice" class="flex-1">
            <el-input-number
              v-model="formData.purchasePrice"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              placeholder="0.00"
              class="!w-full"
            />
          </el-form-item>
          <el-form-item label="销售价" prop="salePrice" class="flex-1" label-width="70px">
            <el-input-number
              v-model="formData.salePrice"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
              placeholder="0.00"
              class="!w-full"
            />
          </el-form-item>
        </div>
        <div class="flex gap-4">
          <el-form-item label="补货阈值" prop="restockThreshold" class="flex-1">
            <el-input-number
              v-model="formData.restockThreshold"
              :min="0"
              controls-position="right"
              class="!w-full"
            />
          </el-form-item>
          <el-form-item v-if="!formData.id" label="初始库存" class="flex-1">
            <el-input-number v-model="formData.stock" :min="0" controls-position="right" class="!w-full" />
          </el-form-item>
        </div>
        <el-form-item label="商品描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="quoteDialogVisible"
      title="供应商报价与补货提醒"
      width="760"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="space-y-4">
        <div class="rounded-2xl bg-amber-50 px-4 py-3">
          <p class="font-medium text-gray-800">{{ currentProduct?.name || '-' }}</p>
          <p class="mt-1 text-sm text-gray-500">
            当前库存 {{ currentProduct?.stock ?? '-' }}，补货阈值 {{ getRestockThreshold(currentProduct) }}，
            当前状态 {{ getRestockStatusMeta(currentProduct).label }}。
          </p>
        </div>
        <el-input
          v-model="restockContent"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="请输入补货提醒内容"
          :disabled="isRestocking(currentProduct)"
        />
        <el-table :data="quoteList" v-loading="quoteLoading" stripe>
          <el-table-column prop="supplierName" label="供应商" min-width="180" />
          <el-table-column prop="productName" label="商品" min-width="160" />
          <el-table-column prop="productSpec" label="规格型号" min-width="140" show-overflow-tooltip />
          <el-table-column label="供货价" width="120" align="right">
            <template #default="{ row }">{{ formatPrice(row.supplyPrice) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button
                v-if="!isRestocking(currentProduct)"
                type="primary"
                text
                size="small"
                @click="handleSendRestock(row)"
              >
                发送提醒
              </el-button>
              <span v-else class="text-xs text-gray-400">补货中</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!quoteLoading && !quoteList.length"
          description="当前商品暂无已审核通过的供应商报价"
          :image-size="90"
        />
      </div>
      <template #footer>
        <el-button @click="quoteDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
