import request from '@/utils/request'

/** 获取全部分类列表 */
export function getCategoryList(params) {
  return request.get('/category/list', { params })
}

/** 新增分类 */
export function addCategory(data) {
  return request.post('/category', data)
}

/** 修改分类 */
export function updateCategory(data) {
  return request.put('/category', data)
}

/** 启用或停用分类 */
export function updateCategoryStatus(id, status) {
  return request.put(`/category/${id}/status/${status}`)
}

/** 迁移分类下商品 */
export function migrateCategoryProducts(id, targetCategoryId) {
  return request.put(`/category/${id}/migrate/${targetCategoryId}`)
}

/** 删除分类 */
export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}
