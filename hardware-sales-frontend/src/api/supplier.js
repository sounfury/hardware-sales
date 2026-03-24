import request from '@/utils/request'

/** 分页查询供应商 */
export function getSupplierPage(params) {
  return request.get('/supplier/page', { params })
}

/** 查询供应商详情 */
export function getSupplier(id) {
  return request.get(`/supplier/${id}`)
}

/** 新增供应商 */
export function addSupplier(data) {
  return request.post('/supplier', data)
}

/** 修改供应商 */
export function updateSupplier(data) {
  return request.put('/supplier', data)
}

/** 删除供应商 */
export function deleteSupplier(id) {
  return request.delete(`/supplier/${id}`)
}

/** 审核供应商 */
export function auditSupplier(id, params) {
  return request.put(`/supplier/audit/${id}`, null, { params })
}
