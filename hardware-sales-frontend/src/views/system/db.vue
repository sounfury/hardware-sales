<script setup>
import { ElMessage } from 'element-plus'
import { downloadDbBackup } from '@/api/system'

const backupLoading = ref(false)

/**
 * 手动触发数据库备份下载，直接导出后端返回的 SQL 文件。
 */
async function handleBackup() {
  backupLoading.value = true
  try {
    const blob = await downloadDbBackup()
    const fileName = `hardware_sales_backup_${Date.now()}.sql`
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('数据库备份文件已开始下载')
  } finally {
    backupLoading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <div>
      <h2 class="text-lg font-semibold text-gray-800">数据库备份</h2>
      <p class="mt-0.5 text-sm text-gray-400">支持导出 SQL 备份文件，具体执行方式由后端数据库配置决定</p>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="rounded-2xl border border-emerald-100 bg-emerald-50 px-5 py-5">
        <p class="text-base font-medium text-gray-800">数据库备份</p>
        <p class="mt-2 text-sm leading-6 text-gray-500">
          管理员点击后，系统会按后端当前配置导出 `hardware_sales` 数据库的 SQL 文件。
        </p>
        <el-button
          type="primary"
          class="mt-4"
          :loading="backupLoading"
          @click="handleBackup"
        >
          立即备份
        </el-button>
      </div>
    </el-card>
  </div>
</template>
