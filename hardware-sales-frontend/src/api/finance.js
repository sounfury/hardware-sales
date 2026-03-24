import request from '@/utils/request'

/** 分页查询财务记录 */
export function getFinancePage(params) {
  return request.get('/finance/page', { params })
}

/** 查询应收金额 */
export function getReceivableAmount() {
  return request.get('/finance/receivable')
}

/** 查询应付金额 */
export function getPayableAmount() {
  return request.get('/finance/payable')
}

/** 查询收支汇总 */
export function getFinanceSummary() {
  return request.get('/finance/summary')
}
