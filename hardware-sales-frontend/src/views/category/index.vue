<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCategoryList,
  addCategory,
  updateCategory,
  updateCategoryStatus,
  migrateCategoryProducts,
  deleteCategory,
} from '@/api/category'

const loading = ref(false)
const tableData = ref([])

/** 新增/编辑对话框状态 */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  name: '',
  sort: 0,
})
const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

/** 迁移商品对话框 */
const migrateDialogVisible = ref(false)
const migrateFormRef = ref(null)
const migrateLoading = ref(false)
const migratingRow = ref(null)
const migrateForm = reactive({
  targetCategoryId: null,
})
const migrateRules = {
  targetCategoryId: [{ required: true, message: '请选择目标分类', trigger: 'change' }],
}

const activeCategoryOptions = computed(() =>
  tableData.value.filter((item) => item.status === 1 && item.id !== migratingRow.value?.id),
)

/** 加载分类列表 */
async function loadList() {
  loading.value = true
  try {
    const res = await getCategoryList()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formData.id = null
  formData.name = ''
  formData.sort = 0
}

/** 打开新增对话框 */
function handleAdd() {
  dialogTitle.value = '新增分类'
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

/** 打开编辑对话框 */
function handleEdit(row) {
  dialogTitle.value = '编辑分类'
  formData.id = row.id
  formData.name = row.name
  formData.sort = row.sort
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

/** 提交表单 */
async function handleSubmit() {
  await formRef.value.validate()
  if (formData.id) {
    await updateCategory({ id: formData.id, name: formData.name, sort: formData.sort })
    ElMessage.success('分类修改成功')
  } else {
    await addCategory({ name: formData.name, sort: formData.sort })
    ElMessage.success('分类创建成功')
  }
  dialogVisible.value = false
  await loadList()
}

function getStatusMeta(status) {
  return Number(status) === 1
    ? { label: '启用中', type: 'success' }
    : { label: '已停用', type: 'info' }
}

function canDelete(row) {
  return Number(row.status) === 0 && Number(row.productCount || 0) === 0
}

function canMigrate(row) {
  return Number(row.productCount || 0) > 0 && activeCategoryOptions.value.length > 0
}

async function handleToggleStatus(row) {
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '停用'
  const message = nextStatus === 1
    ? `确认重新启用分类【${row.name}】？`
    : `确认停用分类【${row.name}】？停用后不能再分配给新商品。`
  await ElMessageBox.confirm(message, `${actionText}分类`, { type: 'warning' })
  await updateCategoryStatus(row.id, nextStatus)
  ElMessage.success(`分类已${actionText}`)
  await loadList()
}

function openMigrateDialog(row) {
  migratingRow.value = row
  migrateForm.targetCategoryId = null
  migrateDialogVisible.value = true
  nextTick(() => migrateFormRef.value?.clearValidate())
}

async function handleMigrate() {
  await migrateFormRef.value.validate()
  if (!migratingRow.value) return
  migrateLoading.value = true
  try {
    const res = await migrateCategoryProducts(migratingRow.value.id, migrateForm.targetCategoryId)
    ElMessage.success(`已迁移 ${res.data || 0} 个商品`)
    migrateDialogVisible.value = false
    await loadList()
  } finally {
    migrateLoading.value = false
  }
}

/** 删除分类 */
async function handleDelete(row) {
  await deleteCategory(row.id)
  ElMessage.success(`分类【${row.name}】已删除`)
  await loadList()
}

onMounted(() => loadList())
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-800">商品分类管理</h2>
        <p class="text-sm text-gray-400 mt-0.5">分类支持停用、商品迁移，只有空且停用的分类才允许删除</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon class="mr-1"><Plus /></el-icon>新增分类
      </el-button>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="删除规则：不会删除商品；请先迁移商品，再删除停用的空分类。"
    />

    <el-card shadow="never" class="!border-gray-200/80">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="分类名称" min-width="180">
          <template #default="{ row }">
            <span class="font-medium text-gray-700">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center">
          <template #default="{ row }">
            <span class="inline-flex items-center justify-center w-8 h-6 rounded bg-gray-100 text-xs text-gray-500 font-mono">{{ row.sort }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type" effect="plain">
              {{ getStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商品数" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono text-sm" :class="Number(row.productCount || 0) > 0 ? 'text-amber-600' : 'text-gray-500'">
              {{ row.productCount || 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" min-width="280" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-button type="primary" text size="small" @click="handleEdit(row)">
                <el-icon class="mr-0.5"><Edit /></el-icon>编辑
              </el-button>
              <el-button
                :type="Number(row.status) === 1 ? 'warning' : 'success'"
                text
                size="small"
                @click="handleToggleStatus(row)"
              >
                <el-icon class="mr-0.5"><SwitchButton /></el-icon>{{ Number(row.status) === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button
                type="primary"
                text
                size="small"
                :disabled="!canMigrate(row)"
                @click="openMigrateDialog(row)"
              >
                迁移商品
              </el-button>
              <template v-if="canDelete(row)">
                <el-popconfirm
                  :title="`确定删除分类【${row.name}】？`"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleDelete(row)"
                >
                  <template #reference>
                    <el-button type="danger" text size="small">
                      <el-icon class="mr-0.5"><Delete /></el-icon>删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </template>
              <el-tooltip v-else content="仅允许删除已停用且无商品的空分类" placement="top">
                <span>
                  <el-button type="danger" text size="small" disabled>
                    <el-icon class="mr-0.5"><Delete /></el-icon>删除
                  </el-button>
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="460"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
        label-position="right"
        class="pr-4"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" maxlength="30" clearable />
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" controls-position="right" class="!w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="migrateDialogVisible"
      title="迁移分类下商品"
      width="460"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <div class="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
        源分类：{{ migratingRow?.name || '-' }}，当前商品数：{{ migratingRow?.productCount || 0 }}
      </div>
      <el-form
        ref="migrateFormRef"
        :model="migrateForm"
        :rules="migrateRules"
        label-width="90px"
        class="pr-4"
      >
        <el-form-item label="目标分类" prop="targetCategoryId">
          <el-select v-model="migrateForm.targetCategoryId" placeholder="请选择目标分类" class="!w-full">
            <el-option
              v-for="item in activeCategoryOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="migrateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="migrateLoading" @click="handleMigrate">确认迁移</el-button>
      </template>
    </el-dialog>
  </div>
</template>
