<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)

/** 菜单配置 */
const menus = [
  {
    title: '首页',
    icon: 'HomeFilled',
    index: '/dashboard',
  },
  {
    title: '商品管理',
    icon: 'Goods',
    children: [
      { title: '商品分类', index: '/product/category' },
      { title: '商品列表', index: '/product/list' },
    ],
  },
  {
    title: '供应商管理',
    icon: 'UserFilled',
    children: [
      { title: '供应商列表', index: '/supplier/list' },
      { title: '供应商商品', index: '/supplier/product' },
      { title: '站内消息', index: '/supplier/message' },
    ],
  },
  {
    title: '进销存',
    icon: 'ShoppingCart',
    children: [
      { title: '采购管理', index: '/trade/purchase' },
      { title: '销售管理', index: '/trade/sales' },
      { title: '库存流水', index: '/trade/inventory' },
    ],
  },
  {
    title: '财务管理',
    icon: 'Money',
    index: '/finance',
  },
  {
    title: '系统管理',
    icon: 'Setting',
    children: [
      { title: '用户管理', index: '/system/user' },
      { title: '数据库备份', index: '/system/db' },
    ],
  },
]

/** 退出登录 */
async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
  await userStore.logout()
  router.push('/login')
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {
      userStore.logout()
      router.push('/login')
    })
  }
})
</script>

<template>
  <div class="h-full flex overflow-hidden bg-content-bg">
    <!-- 侧边栏 -->
    <aside
      class="h-full flex flex-col bg-sidebar shrink-0 transition-all duration-300 overflow-hidden relative z-20 shadow-premium"
      :style="{ width: isCollapse ? '72px' : '240px' }"
    >
      <!-- Logo -->
      <div class="h-[64px] flex items-center justify-center bg-sidebar-logo shrink-0 px-4 mb-2">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center border border-white/10">
            <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <span v-if="!isCollapse" class="text-white text-[16px] font-bold tracking-tight whitespace-nowrap">
            五金销售管理
          </span>
        </div>
      </div>

      <!-- 菜单 -->
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#ffffff"
        class="flex-1 overflow-y-auto! border-r-0! px-2 pb-4"
      >
        <template v-for="menu in menus" :key="menu.title">
          <el-menu-item v-if="!menu.children" :index="menu.index" class="!rounded-xl !mb-1">
            <el-icon :size="20"><component :is="menu.icon" /></el-icon>
            <template #title><span class="font-medium ml-1">{{ menu.title }}</span></template>
          </el-menu-item>
          
          <el-sub-menu v-else :index="menu.title" class="!mb-1">
            <template #title>
              <el-icon :size="20"><component :is="menu.icon" /></el-icon>
              <span class="font-medium ml-1">{{ menu.title }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.index"
              :index="child.index"
              class="!rounded-xl !mb-1"
            >
              <template #title><span class="ml-1">{{ child.title }}</span></template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>

      <!-- 底部折叠按钮 (可选，目前在 Header 中) -->
    </aside>

    <!-- 右侧区域 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 顶栏 -->
      <header class="h-[64px] shrink-0 bg-white/80 backdrop-blur-md border-b border-slate-200/60 flex items-center justify-between px-6 sticky top-0 z-10">
        <div class="flex items-center gap-4">
          <button
            class="w-10 h-10 flex items-center justify-center rounded-xl hover:bg-slate-100 text-slate-500 transition-all cursor-pointer active:scale-95"
            @click="isCollapse = !isCollapse"
          >
            <el-icon :size="20">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </button>
          
          <!-- 面包屑或其他内容可以放这里 -->
          <div class="hidden sm:block">
            <span class="text-xs font-medium text-slate-400 uppercase tracking-wider">Workspace</span>
            <h2 class="text-sm font-semibold text-slate-800">{{ route.meta.title || '控制台' }}</h2>
          </div>
        </div>

        <div class="flex items-center gap-4">
          <div class="flex items-center gap-3 px-3 py-1.5 rounded-xl bg-slate-50 border border-slate-100">
            <div class="w-7 h-7 rounded-lg bg-primary/10 flex items-center justify-center">
              <el-icon :size="14" class="text-primary"><UserFilled /></el-icon>
            </div>
            <span class="text-sm font-medium text-slate-700">
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}
            </span>
          </div>
          
          <div class="w-px h-6 bg-slate-200"></div>
          
          <el-button
            circle
            class="!border-none hover:!bg-rose-50 hover:!text-rose-500 !text-slate-400 transition-all"
            @click="handleLogout"
          >
            <el-icon :size="18"><SwitchButton /></el-icon>
          </el-button>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="flex-1 overflow-auto p-6 scroll-smooth">
        <div class="max-w-[1600px] mx-auto">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 侧边栏菜单样式覆盖 */
:deep(.el-menu) {
  border: none;
}

:deep(.el-menu-item) {
  height: 48px !important;
  line-height: 48px !important;
  margin: 4px 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

:deep(.el-menu-item:hover) {
  background-color: var(--color-sidebar-hover) !important;
  color: #fff !important;
}

:deep(.el-menu-item.is-active) {
  background-color: var(--color-sidebar-active) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.2);
}

:deep(.el-sub-menu__title) {
  height: 48px !important;
  line-height: 48px !important;
  border-radius: 12px !important;
  margin: 4px 0;
  transition: all 0.3s !important;
}

:deep(.el-sub-menu__title:hover) {
  background-color: var(--color-sidebar-hover) !important;
  color: #fff !important;
}

:deep(.el-menu--inline) {
  background-color: transparent !important;
  padding: 4px 0 4px 12px !important;
}

/* 渐变过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
