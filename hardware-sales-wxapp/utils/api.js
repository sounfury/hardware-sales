const { request } = require('./request.js')

// 1. 认证模块
const authApi = {
    /** 小程序登录。 */
    login: (data) => request({ url: '/api/auth/miniapp/login', method: 'POST', data }),
    /** 小程序注册。 */
    register: (data) => request({ url: '/api/auth/miniapp/register', method: 'POST', data }),
    /** 退出登录。 */
    logout: () => request({ url: '/api/auth/logout', method: 'POST' }),
    /** 获取当前登录用户信息。 */
    info: () => request({ url: '/api/auth/info', method: 'GET' })
}

// 2. 供应商资料模块
const supplierApi = {
    /** 查询供应商分页。 */
    page: (params) => request({ url: '/api/supplier/page', method: 'GET', data: params }),
    /** 查询当前登录用户的供应商资料。 */
    current: () => request({ url: '/api/supplier/current', method: 'GET' }),
    /** 按 ID 查询供应商资料。 */
    getById: (id) => request({ url: `/api/supplier/${id}`, method: 'GET' }),
    /** 新增供应商资料。 */
    create: (data) => request({ url: '/api/supplier', method: 'POST', data }),
    /** 修改供应商资料。 */
    update: (data) => request({ url: '/api/supplier', method: 'PUT', data })
}

// 3. 商品原型模块
const productApi = {
    /** 查询商品分页。 */
    page: (params) => request({ url: '/api/product/page', method: 'GET', data: params }),
    /** 查询商品详情。 */
    getById: (id) => request({ url: `/api/product/${id}`, method: 'GET' })
}

// 4. 供应商商品报价模块
const supplierProductApi = {
    /** 查询我的报价分页。 */
    page: (params) => request({ url: '/api/supplier-product/page', method: 'GET', data: params }),
    /** 查询报价详情。 */
    getById: (id) => request({ url: `/api/supplier-product/${id}`, method: 'GET' }),
    /** 查询供应商全部报价。 */
    list: (supplierId) => request({ url: '/api/supplier-product/list', method: 'GET', data: { supplierId } }),
    /** 新增报价。 */
    create: (data) => request({ url: '/api/supplier-product', method: 'POST', data }),
    /** 修改报价。 */
    update: (data) => request({ url: '/api/supplier-product', method: 'PUT', data }),
    /** 删除报价。 */
    delete: (id) => request({ url: `/api/supplier-product/${id}`, method: 'DELETE' })
}

// 5. 站内消息模块
const messageApi = {
    /** 查询消息分页。 */
    page: (params) => request({ url: '/api/message/page', method: 'GET', data: params }),
    /** 查询未读消息数。 */
    getUnreadCount: (receiverId) => request({ url: '/api/message/unread-count', method: 'GET', data: { receiverId } }),
    /** 标记单条消息已读。 */
    markRead: (id) => request({ url: `/api/message/read/${id}`, method: 'PUT' }),
    /** 标记全部消息已读。 */
    markAllRead: (receiverId) => request({ url: '/api/message/read-all', method: 'PUT', data: { receiverId } }),
    /** 回复补货提醒。 */
    restockReply: (data) => request({ url: '/api/message/restock-reply', method: 'POST', data })
}

// 6. 小程序客户预定模块
const salesMiniappApi = {
    /** 客户提交预定。 */
    preorder: (data) => request({ url: '/api/sales/miniapp/preorder', method: 'POST', data }),
    /** 查询当前客户的预定分页。 */
    myPage: (params) => request({ url: '/api/sales/miniapp/my-page', method: 'GET', data: params }),
    /** 查询当前客户的预定详情。 */
    getById: (id) => request({ url: `/api/sales/miniapp/${id}`, method: 'GET' })
}

module.exports = {
    authApi,
    supplierApi,
    productApi,
    supplierProductApi,
    messageApi,
    salesMiniappApi
}
