import request from '@/utils/request'

/** 分页查询库存流水 */
export function getInventoryLogPage(params) {
  return request.get('/inventory/log/page', { params })
}
