import request from '@/utils/request'

/** 分页查询用户 */
export function getUserPage(params) {
  return request.get('/system/user/page', { params })
}

/** 查询用户详情 */
export function getUserDetail(id) {
  return request.get(`/system/user/${id}`)
}

/** 新增用户 */
export function addUser(data) {
  return request.post('/system/user', data)
}

/** 修改用户 */
export function updateUser(data) {
  return request.put('/system/user', data)
}

/** 删除用户 */
export function deleteUser(id) {
  return request.delete(`/system/user/${id}`)
}

/** 重置密码 */
export function resetUserPassword(id, newPassword) {
  return request.put(`/system/user/reset-password/${id}`, null, {
    params: { newPassword },
  })
}

/** 下载数据库备份 */
export function downloadDbBackup() {
  return request.get('/system/db/backup', {
    responseType: 'blob',
  })
}

/** 上传数据库备份文件还原 */
export function restoreDbBackup(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/system/db/restore', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
