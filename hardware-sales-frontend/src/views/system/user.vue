<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserPage,
  addUser,
  updateUser,
  deleteUser,
  resetUserPassword,
} from '@/api/system'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  username: '',
  role: '',
})

const roleOptions = [
  { label: '业务管理员', value: 'ADMIN' },
  { label: '供应商', value: 'SUPPLIER' },
  { label: '客户', value: 'CUSTOMER' },
]

const statusOptions = [
  { label: '正常', value: 1, type: 'success' },
  { label: '禁用', value: 0, type: 'danger' },
]

function getRoleLabel(role) {
  return roleOptions.find((item) => item.value === role)?.label || role || '-'
}

function getStatusMeta(status) {
  return statusOptions.find((item) => item.value === status) || { label: '未知', type: 'info' }
}

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const formData = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  role: 'ADMIN',
  status: 1,
})

const formRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: formData.id
    ? []
    : [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}))

async function loadList() {
  loading.value = true
  try {
    const res = await getUserPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: searchForm.username || undefined,
      role: searchForm.role || undefined,
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
  searchForm.username = ''
  searchForm.role = ''
  handleSearch()
}

function resetForm() {
  formData.id = null
  formData.username = ''
  formData.password = ''
  formData.nickname = ''
  formData.phone = ''
  formData.role = 'ADMIN'
  formData.status = 1
}

function handleAdd() {
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEdit(row) {
  dialogTitle.value = '编辑用户'
  Object.assign(formData, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    phone: row.phone,
    role: row.role,
    status: row.status,
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = {
    id: formData.id,
    username: formData.username,
    password: formData.password || undefined,
    nickname: formData.nickname,
    phone: formData.phone,
    role: formData.role,
    status: formData.status,
  }
  if (payload.id) {
    delete payload.password
    await updateUser(payload)
    ElMessage.success('用户信息更新成功')
  } else {
    await addUser(payload)
    ElMessage.success('用户创建成功')
  }
  dialogVisible.value = false
  loadList()
}

async function handleDelete(row) {
  if (row.id === userStore.userInfo?.id) {
    ElMessage.warning('不能删除当前登录账号')
    return
  }
  await deleteUser(row.id)
  ElMessage.success('用户删除成功')
  loadList()
}

async function handleResetPassword(row) {
  const { value } = await ElMessageBox.prompt(`请输入 ${row.username} 的新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码长度至少 6 位',
  })
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

onMounted(() => loadList())
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-800">用户管理</h2>
        <p class="mt-0.5 text-sm text-gray-400">维护业务管理员、供应商和客户登录账号</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon class="mr-1"><Plus /></el-icon>新增用户
      </el-button>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
        <el-input
          v-model="searchForm.username"
          placeholder="用户名"
          clearable
          class="!w-48"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="searchForm.role" placeholder="角色" clearable class="!w-40">
          <el-option
            v-for="item in roleOptions"
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
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type" effect="plain">
              {{ getStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" text size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-popconfirm
              title="确定删除该用户？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
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
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="84px" class="pr-4">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="!!formData.id" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!formData.id" label="初始密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <div class="grid gap-4 md:grid-cols-2">
          <el-form-item label="角色" prop="role">
            <el-select v-model="formData.role" placeholder="请选择角色" class="!w-full">
              <el-option
                v-for="item in roleOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="formData.status" placeholder="请选择状态" class="!w-full">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
