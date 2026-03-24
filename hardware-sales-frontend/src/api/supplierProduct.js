import request from '@/utils/request'

/** 分页查询供应商商品 */
export function getSupplierProductPage(params) {
  return request.get('/supplier-product/page', { params })
}

/** 查询供应商商品详情 */
export function getSupplierProduct(id) {
  return request.get(`/supplier-product/${id}`)
}

/** 查询指定供应商全部供货商品 */
export function getSupplierProductList(supplierId) {
  return request.get('/supplier-product/list', { params: { supplierId } })
}

/** 查询指定商品的可供货供应商 */
export function getSupplierProductByProduct(productId) {
  return request.get('/supplier-product/by-product', { params: { productId } })
}

/** 新增供应商商品 */
export function addSupplierProduct(data) {
  return request.post('/supplier-product', data)
}

/** 修改供应商商品 */
export function updateSupplierProduct(data) {
  return request.put('/supplier-product', data)
}

/** 删除供应商商品 */
export function deleteSupplierProduct(id) {
  return request.delete(`/supplier-product/${id}`)
}
