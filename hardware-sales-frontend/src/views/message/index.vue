<script setup>
import { ElMessage } from 'element-plus'
import {
  getMessagePage,
  sendMessage,
  markMessageRead,
  markAllMessageRead,
  getUnreadCount,
} from '@/api/message'
import { getSupplierPage } from '@/api/supplier'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const unreadCount = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
let unreadTimer = null

const searchForm = reactive({
  isRead: null,
  type: '',
})

const supplierOptions = ref([])
const receiverLabelMap = computed(() =>
  supplierOptions.value.reduce((acc, item) => {
    acc[item.userId] = item.companyName
    return acc
  }, {}),
)

const currentUserId = computed(() => userStore.userInfo?.id || null)

const dialogVisible = ref(false)
const formRef = ref(null)
const formData = reactive({
  receiverId: null,
  title: '',
  content: '',
})
const messageTypeMap = {
  GENERAL: { label: '普通消息', type: 'info' },
  RESTOCK_NOTICE: { label: '补货提醒', type: 'warning' },
  RESTOCK_REPLY: { label: '补货回复', type: 'success' },
}

const formRules = {
  receiverId: [{ required: true, message: '请选择接收供应商', trigger: 'change' }],
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
}

async function ensureUserInfo() {
  if (!userStore.userInfo && userStore.isLoggedIn) {
    await userStore.fetchUserInfo()
  }
}

async function loadSuppliers() {
  const res = await getSupplierPage({
    pageNum: 1,
    pageSize: 200,
    auditStatus: 1,
  })
  supplierOptions.value = res.data.records || []
}

async function loadUnreadCount() {
  if (!currentUserId.value) return
  const res = await getUnreadCount(currentUserId.value)
  unreadCount.value = res.data || 0
}

async function loadList() {
  if (!currentUserId.value) return
  loading.value = true
  try {
    const res = await getMessagePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      receiverId: currentUserId.value,
      isRead: searchForm.isRead ?? undefined,
      type: searchForm.type || undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadList(), loadUnreadCount()])
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  searchForm.isRead = null
  searchForm.type = ''
  handleSearch()
}

function handleOpenDialog() {
  formData.receiverId = null
  formData.title = ''
  formData.content = ''
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function handleSend() {
  await formRef.value.validate()
  await sendMessage({
    receiverId: formData.receiverId,
    title: formData.title,
    content: formData.content,
  })
  ElMessage.success('消息发送成功')
  dialogVisible.value = false
  loadUnreadCount()
}

async function handleMarkRead(row) {
  await markMessageRead(row.id)
  ElMessage.success('消息已标记为已读')
  reloadAll()
}

async function handleMarkAllRead() {
  await markAllMessageRead(currentUserId.value)
  ElMessage.success('已全部标记为已读')
  reloadAll()
}

function getMessageTypeMeta(type) {
  return messageTypeMap[type] || { label: type || '未知类型', type: 'info' }
}

function getReceiverLabel(row) {
  if (row.receiverId === currentUserId.value) {
    return userStore.userInfo?.nickname || userStore.userInfo?.username || '当前用户'
  }
  return receiverLabelMap.value[row.receiverId] || `用户#${row.receiverId}`
}

function startUnreadPolling() {
  if (unreadTimer || !currentUserId.value) return
  // 只轮询未读数，避免整页频繁刷新造成额外噪音。
  unreadTimer = window.setInterval(() => {
    loadUnreadCount()
  }, 30000)
}

function stopUnreadPolling() {
  if (!unreadTimer) return
  window.clearInterval(unreadTimer)
  unreadTimer = null
}

onMounted(async () => {
  await ensureUserInfo()
  await loadSuppliers()
  await reloadAll()
  startUnreadPolling()
})

onBeforeUnmount(() => {
  stopUnreadPolling()
})
</script>

<template>
  <div class="space-y-4">
    <div class="grid gap-4 md:grid-cols-3">
      <el-card shadow="never" class="!border-gray-200/80">
        <p class="text-sm text-gray-400">当前收件箱</p>
        <p class="mt-2 text-xl font-semibold text-gray-800">
          {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录' }}
        </p>
      </el-card>
      <el-card shadow="never" class="!border-gray-200/80">
        <p class="text-sm text-gray-400">未读消息</p>
        <p class="mt-2 text-2xl font-semibold text-amber-500">{{ unreadCount }}</p>
      </el-card>
      <el-card shadow="never" class="!border-gray-200/80">
        <p class="text-sm text-gray-400">发送对象</p>
        <p class="mt-2 text-sm text-gray-600">管理员可向已审核供应商发提醒，并查看供应商补货回复</p>
      </el-card>
    </div>

    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-800">站内消息</h2>
        <p class="mt-0.5 text-sm text-gray-400">当前列表展示我的收件箱，重点跟踪补货提醒和供应商回复</p>
      </div>
      <div class="flex items-center gap-2">
        <el-button @click="handleMarkAllRead">
          <el-icon class="mr-1"><Check /></el-icon>全部标已读
        </el-button>
        <el-button type="primary" @click="handleOpenDialog">
          <el-icon class="mr-1"><Promotion /></el-icon>发送消息
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="mb-4 flex flex-wrap items-center gap-3 border-b border-gray-100 pb-4">
        <el-select
          v-model="searchForm.isRead"
          placeholder="消息状态"
          clearable
          class="!w-40"
        >
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-select
          v-model="searchForm.type"
          placeholder="消息类型"
          clearable
          class="!w-40"
        >
          <el-option label="普通消息" value="GENERAL" />
          <el-option label="补货提醒" value="RESTOCK_NOTICE" />
          <el-option label="补货回复" value="RESTOCK_REPLY" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon class="mr-1"><Search /></el-icon>筛选
        </el-button>
        <el-button @click="handleReset">
          <el-icon class="mr-1"><Refresh /></el-icon>重置
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'info' : 'warning'" effect="plain">
              {{ row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getMessageTypeMeta(row.type).type" effect="plain">
              {{ getMessageTypeMeta(row.type).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="senderName" label="发送人" width="120" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="关联商品" min-width="160">
          <template #default="{ row }">{{ row.productName || '-' }}</template>
        </el-table-column>
        <el-table-column label="回复关联" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.replyToTitle ? `回复：${row.replyToTitle}` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="接收主体" min-width="160">
          <template #default="{ row }">{{ getReceiverLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="发送时间" width="180" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isRead" type="primary" text size="small" @click="handleMarkRead(row)">
              标记已读
            </el-button>
            <span v-else class="text-xs text-gray-400">已处理</span>
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
      title="发送站内消息"
      width="560"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px" class="pr-4">
        <el-form-item label="接收供应商" prop="receiverId">
          <el-select v-model="formData.receiverId" placeholder="请选择供应商" class="!w-full" filterable>
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入消息标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请输入消息内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>
