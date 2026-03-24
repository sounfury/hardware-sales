<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadDbBackup, restoreDbBackup } from '@/api/system'

const backupLoading = ref(false)
const restoreLoading = ref(false)
const selectedFile = ref(null)

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

/**
 * 只保留用户选择的 SQL 文件，实际上传由“开始还原”按钮触发。
 */
function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile?.raw || null
}

/**
 * 在用户确认后执行数据库还原，避免误操作直接覆盖当前数据。
 */
async function handleRestore() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要还原的 SQL 文件')
    return
  }
  await ElMessageBox.confirm(
    '还原会覆盖当前数据库中的业务数据，建议先执行一次备份。是否继续？',
    '数据库还原确认',
    { type: 'warning' },
  )
  restoreLoading.value = true
  try {
    await restoreDbBackup(selectedFile.value)
    ElMessage.success('数据库还原成功')
    selectedFile.value = null
  } finally {
    restoreLoading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <div>
      <h2 class="text-lg font-semibold text-gray-800">数据库备份</h2>
      <p class="mt-0.5 text-sm text-gray-400">支持导出 SQL 备份和手动还原，具体执行方式由后端数据库配置决定</p>
    </div>

    <el-card shadow="never" class="!border-gray-200/80">
      <div class="grid gap-4 lg:grid-cols-2">
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

        <div class="rounded-2xl border border-amber-100 bg-amber-50 px-5 py-5">
          <p class="text-base font-medium text-gray-800">数据库还原</p>
          <p class="mt-2 text-sm leading-6 text-gray-500">
            请上传系统导出的 `.sql` 备份文件。还原会覆盖当前业务数据，建议先执行一次备份。
          </p>
          <el-upload
            class="mt-4"
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            accept=".sql"
            :show-file-list="true"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 SQL 文件到这里，或点击选择文件</div>
          </el-upload>
          <div class="mt-4 flex items-center justify-between gap-4">
            <span class="text-sm text-gray-500">
              {{ selectedFile ? `已选择：${selectedFile.name}` : '尚未选择备份文件' }}
            </span>
            <el-button
              type="warning"
              :loading="restoreLoading"
              @click="handleRestore"
            >
              开始还原
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>
