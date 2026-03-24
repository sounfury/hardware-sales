import request from '@/utils/request'

/** 分页查询商品 */
export function getProductPage(params) {
  return request.get('/product/page', { params })
}

/** 获取商品详情 */
export function getProduct(id) {
  return request.get(`/product/${id}`)
}

/** 新增商品 */
export function addProduct(data) {
  return request.post('/product', data)
}

/** 修改商品 */
export function updateProduct(data) {
  return request.put('/product', data)
}

/** 删除商品 */
export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}

/** 完成补货 */
export function completeRestock(id) {
  return request.put(`/product/restock-complete/${id}`)
}
