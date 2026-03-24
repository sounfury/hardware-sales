import request from '@/utils/request'

/** 分页查询销售单 */
export function getSalesPage(params) {
  return request.get('/sales/page', { params })
}

/** 查询销售单详情 */
export function getSalesDetail(id) {
  return request.get(`/sales/${id}`)
}

/** 新建销售单 */
export function createSalesOrder(data) {
  return request.post('/sales', data)
}

/** 结算销售单 */
export function settleSalesOrder(id) {
  return request.put(`/sales/settle/${id}`)
}
