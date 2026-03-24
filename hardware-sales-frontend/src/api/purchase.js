import request from '@/utils/request'

/** 分页查询采购单 */
export function getPurchasePage(params) {
  return request.get('/purchase/page', { params })
}

/** 查询采购单详情 */
export function getPurchaseDetail(id) {
  return request.get(`/purchase/${id}`)
}

/** 新建采购单 */
export function createPurchaseOrder(data) {
  return request.post('/purchase', data)
}

/** 结算采购单 */
export function settlePurchaseOrder(id) {
  return request.put(`/purchase/settle/${id}`)
}
