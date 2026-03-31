<script setup>
import { ElMessage } from 'element-plus'
import {
  getSupplierPage,
  addSupplier,
  updateSupplier,
  deleteSupplier,
  auditSupplier,
} from '@/api/supplier'
import { getUserPage } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  companyName: '',
  auditStatus: null,
})

const supplierUserOptions = ref([])
const supplierUserMap = computed(() =>
  supplierUserOptions.value.reduce((acc, item) => {
    acc[item.id] = item
    return acc
  }, {}),
)

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  userId: null,
  companyName: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  businessScope: '',
})

const formRules = {
  userId: [{ required: true, message: '请选择关联供应商账号', trigger: 'change' }],
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
}

const auditDialogVisible = ref(false)
const auditFormRef = ref(null)
const auditForm = reactive({
  id: null,
  auditStatus: 1,
  auditRemark: '',
})

const auditRules = {
  auditStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
}

const auditStatusOptions = [
  { label: '待审核', value: 0, type: 'warning' },
  { label: '通过', value: 1, type: 'success' },
  { label: '驳回', value: 2, type: 'danger' },
]

function getAuditMeta(status) {
  return auditStatusOptions.find((item) => item.value === status) || {
    label: '未知',
    type: 'info',
  }
}

/** 判断当前供应商是否还需要显示审核入口。 */
function canAuditSupplier(status) {
  return status !== 1
}

async function loadSupplierUsers() {
  const res = await getUserPage({
    pageNum: 1,
    pageSize: 200,
    role: 'SUPPLIER',
  })
  supplierUserOptions.value = res.data.records || []
}

async function loadList() {
  loading.value = true
  try {
    const res = await getSupplierPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      companyName: searchForm.companyName || undefined,
      auditStatus: searchForm.auditStatus ?? undefined,
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
  searchForm.companyName = ''
  searchForm.auditStatus = null
  handleSearch()
}

function resetForm() {
  formData.id = null
  formData.userId = null
  formData.companyName = ''
  formData.contactPerson = ''
  formData.contactPhone = ''
  formData.address = ''
  formData.businessScope = ''
}

function handleAdd() {
  dialogTitle.value = '新增供应商'
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEdit(row) {
  dialogTitle.value = '编辑供应商'
  Object.assign(formData, {
    id: row.id,
    userId: row.userId,
    companyName: row.companyName,
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    address: row.address,
    businessScope: row.businessScope,
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = { ...formData }
  if (payload.id) {
    await updateSupplier(payload)
    ElMessage.success('供应商更新成功')
  } else {
    await addSupplier(payload)
    ElMessage.success('供应商创建成功')
  }
  dialogVisible.value = false
  loadList()
}

async function handleDelete(id) {
  await deleteSupplier(id)
  ElMessage.success('供应商删除成功')
  loadList()
}

function handleOpenAudit(row) {
  if (!canAuditSupplier(row.auditStatus)) {
    return
  }
  auditForm.id = row.id
  auditForm.auditStatus = row.auditStatus === 2 ? 2 : 1
  auditForm.auditRemark = row.auditRemark || ''
  auditDialogVisible.value = true
  nextTick(() => auditFormRef.value?.clearValidate())
}

async function handleAuditSubmit() {
  await auditFormRef.value.validate()
  await auditSupplier(auditForm.id, {
    auditStatus: auditForm.auditStatus,
    auditRemark: auditForm.auditRemark || undefined,
  })
  ElMessage.success('审核结果已提交')
  auditDialogVisible.value = false
  loadList()
}

onMounted(async () => {
  await loadSupplierUsers()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-800">供应商管理</h2>
        <p class="mt-0.5 text-sm text-gray-400">维护企业档案、审核状态和关联供应商账号</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon class="mr-1"><Plus /></el-icon>新增供应商
      </el-button>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
        <el-input
          v-model="searchForm.companyName"
          placeholder="企业名称"
          clearable
          class="!w-56"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.auditStatus"
          placeholder="审核状态"
          clearable
          class="!w-40"
        >
          <el-option
            v-for="item in auditStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
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
        <el-table-column prop="companyName" label="企业名称" min-width="220" />
        <el-table-column label="关联账号" min-width="180">
          <template #default="{ row }">
            <div class="text-sm text-gray-700">
              <div>{{ supplierUserMap[row.userId]?.nickname || supplierUserMap[row.userId]?.username || '-' }}</div>
              <div class="text-xs text-gray-400">{{ supplierUserMap[row.userId]?.username || '未匹配账号' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="businessScope" label="经营范围" min-width="220" show-overflow-tooltip />
        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getAuditMeta(row.auditStatus).type" effect="plain">
              {{ getAuditMeta(row.auditStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canAuditSupplier(row.auditStatus)"
              type="success"
              text
              size="small"
              @click="handleOpenAudit(row)"
            >
              审核
            </el-button>
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm
              title="确定删除该供应商？"
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
      width="640"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px" class="pr-4">
        <el-form-item label="供应商账号" prop="userId">
          <el-select v-model="formData.userId" placeholder="请选择供应商账号" class="!w-full" filterable>
            <el-option
              v-for="user in supplierUserOptions"
              :key="user.id"
              :label="`${user.nickname || user.username}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="企业名称" prop="companyName">
          <el-input v-model="formData.companyName" placeholder="请输入企业名称" maxlength="100" />
        </el-form-item>
        <div class="flex gap-4">
          <el-form-item label="联系人" class="flex-1">
            <el-input v-model="formData.contactPerson" placeholder="请输入联系人" maxlength="50" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone" class="flex-1">
            <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" maxlength="20" />
          </el-form-item>
        </div>
        <el-form-item label="企业地址">
          <el-input v-model="formData.address" placeholder="请输入企业地址" maxlength="255" />
        </el-form-item>
        <el-form-item label="经营范围">
          <el-input
            v-model="formData.businessScope"
            type="textarea"
            :rows="3"
            placeholder="请输入经营范围"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="auditDialogVisible"
      title="供应商审核"
      width="480"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="80px" class="pr-4">
        <el-form-item label="审核结果" prop="auditStatus">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入审核备注（可选）"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAuditSubmit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>
