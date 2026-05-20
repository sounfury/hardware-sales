import request from '@/utils/request'

/** 分页查询用户 */
export function getUserPage(params) {
  return request.get('/system/user/page', { params })
}

/** 分页查询客户用户，供销售单创建页下拉选择客户。 */
export function getCustomerUserPage(params = {}) {
  return request.get('/system/user/page', {
    params: {
      ...params,
      role: 'CUSTOMER',
    },
  })
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
