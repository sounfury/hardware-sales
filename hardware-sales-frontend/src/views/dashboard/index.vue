<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getFinanceSummary } from '@/api/finance'
import { getMessagePage, getUnreadCount, sendRestockMessage } from '@/api/message'
import { completeRestock, getProductPage } from '@/api/product'
import { getPurchasePage } from '@/api/purchase'
import { getSalesPage } from '@/api/sales'
import { getSupplierProductByProduct } from '@/api/supplierProduct'
import { useUserStore } from '@/stores/user'
import { formatDate, formatMoney } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const summary = reactive({
  totalIncome: 0,
  totalExpense: 0,
  profit: 0,
  receivable: 0,
  payable: 0,
})
const unreadCount = ref(0)
const lowStockProducts = ref([])
const recentPurchases = ref([])
const recentSales = ref([])
const latestInbox = ref([])
const quoteDialogVisible = ref(false)
const quoteLoading = ref(false)
const quoteList = ref([])
const currentProduct = ref(null)
const restockContent = ref('当前商品库存偏低，请尽快安排补货。')

const shortcuts = [
  { title: '新建采购单', path: '/trade/purchase', icon: 'ShoppingTrolley', desc: '录入进货' },
  { title: '新建销售单', path: '/trade/sales', icon: 'Sell', desc: '登记出货' },
  { title: '商品原型', path: '/product/list', icon: 'Goods', desc: '基础资料' },
  { title: '用户管理', path: '/system/user', icon: 'User', desc: '权限管理' },
]

async function ensureUserInfo() {
  if (!userStore.userInfo && userStore.isLoggedIn) {
    await userStore.fetchUserInfo()
  }
}

async function loadData() {
  loading.value = true
  try {
    await ensureUserInfo()
    const tasks = [
      getFinanceSummary(),
      getProductPage({ pageNum: 1, pageSize: 200 }),
      getPurchasePage({ pageNum: 1, pageSize: 5 }),
      getSalesPage({ pageNum: 1, pageSize: 5 }),
    ]
    if (userStore.userInfo?.id) {
      tasks.push(getUnreadCount(userStore.userInfo.id))
      tasks.push(
        getMessagePage({
          pageNum: 1,
          pageSize: 5,
          receiverId: userStore.userInfo.id,
        }),
      )
    }
    const results = await Promise.all(tasks)
    Object.assign(summary, results[0].data)
    lowStockProducts.value = (results[1].data.records || [])
      .filter((item) => isLowStock(item) || isRestocking(item))
      .sort((a, b) => Number(a.stock || 0) - Number(b.stock || 0))
    recentPurchases.value = results[2].data.records || []
    recentSales.value = results[3].data.records || []
    if (userStore.userInfo?.id) {
      unreadCount.value = results[4].data || 0
      latestInbox.value = results[5].data.records || []
    }
  } finally {
    loading.value = false
  }
}

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

async function handleSendRestock(supplierProduct) {
  if (!currentProduct.value) return
  await sendRestockMessage({
    receiverId: supplierProduct.supplierUserId,
    productId: currentProduct.value.id,
    content: restockContent.value,
  })
  ElMessage.success('补货提醒已发送')
  quoteDialogVisible.value = false
  await loadData()
}

async function handleCompleteRestock(product) {
  await ElMessageBox.confirm(`确认将商品【${product.name}】标记为补货完成？`, '提示', {
    type: 'warning',
  })
  await completeRestock(product.id)
  ElMessage.success('补货状态已恢复为正常')
  quoteDialogVisible.value = false
  await loadData()
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

onMounted(() => loadData())
</script>

<template>
  <div class="space-y-6" v-loading="loading">
    <!-- 欢迎横幅 -->
    <div class="relative rounded-[2rem] bg-primary overflow-hidden px-8 py-10 text-white shadow-xl shadow-primary/20">
      <div class="absolute top-0 right-0 w-1/3 h-full overflow-hidden pointer-events-none opacity-20">
        <svg class="w-full h-full" viewBox="0 0 200 200" fill="none">
          <circle cx="150" cy="50" r="80" stroke="white" stroke-width="2" />
          <circle cx="180" cy="120" r="60" stroke="white" stroke-width="2" />
        </svg>
      </div>
      <div class="relative z-10 flex flex-wrap items-center justify-between gap-6">
        <div>
          <p class="text-slate-400 font-medium mb-1">Welcome back,</p>
          <h2 class="text-4xl font-bold tracking-tight">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}
          </h2>
          <p class="mt-3 text-slate-300 max-w-md leading-relaxed">
            您有 <span class="text-white font-bold">{{ unreadCount }}</span> 条消息及 
            <span class="text-amber-400 font-bold">{{ lowStockProducts.length }}</span> 个预警商品待处理。
          </p>
        </div>
        <div class="flex gap-4">
          <button
            v-for="item in shortcuts"
            :key="item.path"
            @click="router.push(item.path)"
            class="group flex items-center gap-3 bg-white/10 hover:bg-white/20 backdrop-blur-md px-5 py-3 rounded-2xl border border-white/10 transition-all active:scale-95"
          >
            <div class="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center text-white group-hover:scale-110 transition-transform">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <div class="text-left">
              <p class="text-sm font-bold text-white">{{ item.title }}</p>
              <p class="text-[10px] text-white/40 uppercase tracking-widest">{{ item.desc }}</p>
            </div>
          </button>
        </div>
      </div>
    </div>

    <!-- 统计指标 -->
    <div class="grid gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-5">
      <div v-for="card in [
        { label: '总收入', value: summary.totalIncome, color: 'text-emerald-600', bg: 'bg-emerald-50', icon: 'TrendCharts' },
        { label: '总支出', value: summary.totalExpense, color: 'text-rose-500', bg: 'bg-rose-50', icon: 'Histogram' },
        { label: '净利润', value: summary.profit, color: 'text-primary', bg: 'bg-slate-50', icon: 'PieChart' },
        { label: '应收金额', value: summary.receivable, color: 'text-amber-500', bg: 'bg-amber-50', icon: 'Wallet' },
        { label: '应付金额', value: summary.payable, color: 'text-indigo-500', bg: 'bg-indigo-50', icon: 'CreditCard' },
      ]" :key="card.label" class="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm">
        <div class="flex items-center gap-3 mb-3">
          <div :class="['w-8 h-8 rounded-lg flex items-center justify-center', card.bg]">
            <el-icon :size="16" :class="card.color"><component :is="card.icon" /></el-icon>
          </div>
          <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ card.label }}</span>
        </div>
        <p class="text-xl font-bold tracking-tight text-slate-800">{{ formatMoney(card.value) }}</p>
      </div>
    </div>

    <!-- 核心监控矩阵 (四列等高) -->
    <div class="grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-4 items-stretch">
      <!-- 1. 库存预警 -->
      <el-card shadow="never" class="!border-none !rounded-3xl flex flex-col h-full">
        <template #header>
          <div class="flex items-center justify-between py-0.5">
            <h3 class="text-base font-bold text-slate-800 flex items-center gap-2">
              <el-icon class="text-amber-500"><Warning /></el-icon>库存预警
            </h3>
            <el-button type="primary" link @click="router.push('/product/list')">More</el-button>
          </div>
        </template>
        <div v-if="lowStockProducts.length" class="space-y-3">
          <div v-for="item in lowStockProducts.slice(0, 5)" :key="item.id" class="p-3 rounded-xl bg-slate-50 border border-slate-100 group hover:bg-white hover:shadow-md transition-all">
            <div class="flex justify-between items-start gap-2 mb-2">
              <p class="font-bold text-slate-700 text-xs truncate">{{ item.name }}</p>
              <span class="text-xs font-black text-amber-500">{{ item.stock }}</span>
            </div>
            <div class="flex justify-between items-center">
              <el-tag :type="getRestockStatusMeta(item).type" size="small" class="!px-2 scale-90 origin-left">{{ getRestockStatusMeta(item).label }}</el-tag>
              <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <el-button circle size="small" icon="Bell" @click="openSupplierDialog(item)" v-if="!isRestocking(item)" />
                <el-button circle size="small" icon="Check" @click="handleCompleteRestock(item)" v-else />
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="库存充足" :image-size="60" />
      </el-card>

      <!-- 2. 最近采购 -->
      <el-card shadow="never" class="!border-none !rounded-3xl flex flex-col h-full">
        <template #header>
          <div class="flex items-center justify-between py-0.5">
            <h3 class="text-base font-bold text-slate-800 flex items-center gap-2">
              <el-icon class="text-primary/50"><Box /></el-icon>最近采购
            </h3>
            <el-button type="primary" link @click="router.push('/trade/purchase')">More</el-button>
          </div>
        </template>
        <div v-if="recentPurchases.length" class="space-y-3">
          <div v-for="item in recentPurchases" :key="item.id" class="p-3 rounded-xl border border-slate-50 hover:bg-slate-50 transition-colors">
            <p class="font-bold text-slate-700 text-xs mb-1 truncate">{{ item.orderNo }}</p>
            <div class="flex justify-between text-[10px] font-medium text-slate-400">
              <span>{{ item.supplierName }}</span>
              <span class="text-slate-700">{{ formatMoney(item.totalAmount) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无记录" :image-size="60" />
      </el-card>

      <!-- 3. 最近销售 -->
      <el-card shadow="never" class="!border-none !rounded-3xl flex flex-col h-full">
        <template #header>
          <div class="flex items-center justify-between py-0.5">
            <h3 class="text-base font-bold text-slate-800 flex items-center gap-2">
              <el-icon class="text-primary/50"><Van /></el-icon>最近销售
            </h3>
            <el-button type="primary" link @click="router.push('/trade/sales')">More</el-button>
          </div>
        </template>
        <div v-if="recentSales.length" class="space-y-3">
          <div v-for="item in recentSales" :key="item.id" class="p-3 rounded-xl border border-slate-50 hover:bg-slate-50 transition-colors">
            <p class="font-bold text-slate-700 text-xs mb-1 truncate">{{ item.customerName || '零售客户' }}</p>
            <div class="flex justify-between text-[10px] font-medium text-slate-400">
              <span>{{ formatDate(item.orderDate) }}</span>
              <span class="text-slate-700">{{ formatMoney(item.totalAmount) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无记录" :image-size="60" />
      </el-card>

      <!-- 4. 最新消息 -->
      <el-card shadow="never" class="!border-none !rounded-3xl flex flex-col h-full">
        <template #header>
          <div class="flex items-center justify-between py-0.5">
            <h3 class="text-base font-bold text-slate-800 flex items-center gap-2">
              <el-icon class="text-primary/50"><Message /></el-icon>最新消息
            </h3>
            <el-button type="primary" link @click="router.push('/supplier/message')">More</el-button>
          </div>
        </template>
        <div v-if="latestInbox.length" class="space-y-3">
          <div v-for="item in latestInbox" :key="item.id" class="p-3 rounded-xl border border-slate-50 hover:bg-slate-50 transition-colors">
            <div class="flex items-center justify-between gap-2 mb-1">
              <p class="truncate font-bold text-slate-700 text-xs">{{ item.title }}</p>
              <span :class="['w-1.5 h-1.5 rounded-full', item.isRead ? 'bg-slate-200' : 'bg-amber-400']"></span>
            </div>
            <p class="line-clamp-1 text-[10px] text-slate-400 leading-relaxed">{{ item.content }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无消息" :image-size="60" />
      </el-card>
    </div>

    <!-- 供应商报价弹窗保持不变 -->
    <el-dialog v-model="quoteDialogVisible" title="供应商报价与补货提醒" width="760" destroy-on-close :close-on-click-modal="false">
      <!-- ... 内容保持不变 ... -->
      <div class="space-y-4">
        <div class="rounded-2xl bg-amber-50 px-4 py-3">
          <p class="font-medium text-gray-800">{{ currentProduct?.name || '-' }}</p>
          <p class="mt-1 text-sm text-gray-500">
            当前库存 {{ currentProduct?.stock ?? '-' }}，补货阈值 {{ getRestockThreshold(currentProduct) }}。
          </p>
        </div>
        <el-input v-model="restockContent" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入补货提醒内容" :disabled="isRestocking(currentProduct)" />
        <el-table :data="quoteList" v-loading="quoteLoading" stripe>
          <el-table-column prop="supplierName" label="供应商" min-width="180" />
          <el-table-column prop="productName" label="商品" min-width="160" />
          <el-table-column label="供货价" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.supplyPrice) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button v-if="!isRestocking(currentProduct)" type="primary" text size="small" @click="handleSendRestock(row)">发送提醒</el-button>
              <span v-else class="text-xs text-gray-400">补货中</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer><el-button @click="quoteDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
