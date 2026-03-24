<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
})
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    if (error?.message === '当前后台仅允许管理员登录') {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="h-full flex items-center justify-center bg-slate-50 relative overflow-hidden">
    <!-- 背景装饰 -->
    <div class="absolute top-0 left-0 w-full h-full overflow-hidden z-0 pointer-events-none">
      <div class="absolute -top-[10%] -left-[10%] w-[40%] h-[40%] bg-blue-500/5 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-[10%] -right-[10%] w-[50%] h-[50%] bg-indigo-500/5 rounded-full blur-3xl"></div>
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full opacity-[0.03]" style="background-image: radial-gradient(#0f172a 1px, transparent 1px); background-size: 32px 32px;"></div>
    </div>

    <!-- 登录容器 -->
    <div class="w-full max-w-[1000px] h-[600px] flex bg-white rounded-[32px] shadow-2xl overflow-hidden relative z-10 mx-6">
      <!-- 左侧：品牌展示 -->
      <div class="hidden lg:flex flex-[1.2] bg-primary relative overflow-hidden flex-col justify-between p-12">
        <!-- 装饰背景 -->
        <div class="absolute top-0 left-0 w-full h-full overflow-hidden">
          <div class="absolute -top-24 -left-24 w-64 h-64 rounded-full bg-white/5 border border-white/10"></div>
          <div class="absolute bottom-12 -right-12 w-48 h-48 rounded-full bg-white/5 border border-white/10"></div>
          <div class="absolute top-1/2 right-12 w-12 h-12 rotate-45 border border-white/10"></div>
        </div>

        <div class="relative z-10">
          <div class="w-14 h-14 rounded-2xl bg-white/10 backdrop-blur-md flex items-center justify-center border border-white/20 mb-8">
            <svg class="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <h1 class="text-white text-4xl font-bold tracking-tight mb-4">五金销售管理</h1>
          <p class="text-slate-400 text-lg max-w-xs leading-relaxed">
            高效、专业、可靠的进销存一体化管理系统
          </p>
        </div>

        <div class="relative z-10 flex items-center gap-6">
          <div v-for="tag in ['进销存', '供应商', '财务分析']" :key="tag" class="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-white/60 text-xs font-medium">
            {{ tag }}
          </div>
        </div>
      </div>

      <!-- 右侧：表单区 -->
      <div class="flex-1 flex flex-col justify-center px-12 md:px-20">
        <div class="mb-10">
          <h2 class="text-2xl font-bold text-slate-800 mb-2">欢迎回来</h2>
          <p class="text-slate-400">请使用您的管理员账号登录</p>
        </div>

        <el-form :model="form" @keyup.enter="handleLogin" label-position="top">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              class="custom-input"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              class="custom-input"
            />
          </el-form-item>
          
          <div class="mt-8">
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="!w-full !h-12 !rounded-xl !text-base shadow-lg shadow-primary/20"
              @click="handleLogin"
            >
              登录系统
            </el-button>
          </div>
        </el-form>

        <div class="mt-12 text-center">
          <p class="text-slate-300 text-xs">&copy; 2026 Hardware Sales Management System</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.custom-input .el-input__wrapper) {
  background-color: #f8fafc !important;
  border-radius: 12px !important;
  box-shadow: none !important;
  border: 1px solid #e2e8f0 !important;
  transition: all 0.3s ease;
}

:deep(.custom-input .el-input__wrapper.is-focus) {
  background-color: #fff !important;
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 4px rgba(15, 23, 42, 0.05) !important;
}

:deep(.el-form-item__label) {
  font-weight: 600 !important;
  color: #475569 !important;
  font-size: 13px !important;
  margin-bottom: 6px !important;
  padding: 0 !important;
}
</style>
