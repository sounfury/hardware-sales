const { request } = require('./request.js')

// 1. 认证模块
const authApi = {
    login: (data) => request({ url: '/api/auth/login', method: 'POST', data }),
    logout: () => request({ url: '/api/auth/logout', method: 'POST' }),
    info: () => request({ url: '/api/auth/info', method: 'GET' })
}

// 2. 供应商资料模块
const supplierApi = {
    page: (params) => request({ url: '/api/supplier/page', method: 'GET', data: params }),
    getById: (id) => request({ url: `/api/supplier/${id}`, method: 'GET' }),
    create: (data) => request({ url: '/api/supplier', method: 'POST', data }),
    update: (data) => request({ url: '/api/supplier', method: 'PUT', data })
}

// 3. 商品原型模块
const productApi = {
    page: (params) => request({ url: '/api/product/page', method: 'GET', data: params }),
    getById: (id) => request({ url: `/api/product/${id}`, method: 'GET' })
}

// 4. 供应商商品报价模块
const supplierProductApi = {
    page: (params) => request({ url: '/api/supplier-product/page', method: 'GET', data: params }),
    getById: (id) => request({ url: `/api/supplier-product/${id}`, method: 'GET' }),
    list: (supplierId) => request({ url: '/api/supplier-product/list', method: 'GET', data: { supplierId } }),
    create: (data) => request({ url: '/api/supplier-product', method: 'POST', data }),
    update: (data) => request({ url: '/api/supplier-product', method: 'PUT', data }),
    delete: (id) => request({ url: `/api/supplier-product/${id}`, method: 'DELETE' })
}

// 5. 站内消息模块
const messageApi = {
    page: (params) => request({ url: '/api/message/page', method: 'GET', data: params }),
    getUnreadCount: (receiverId) => request({ url: '/api/message/unread-count', method: 'GET', data: { receiverId } }),
    markRead: (id) => request({ url: `/api/message/read/${id}`, method: 'PUT' }),
    markAllRead: (receiverId) => request({ url: '/api/message/read-all', method: 'PUT', data: { receiverId } }),
    restockReply: (data) => request({ url: '/api/message/restock-reply', method: 'POST', data })
}

module.exports = {
    authApi,
    supplierApi,
    productApi,
    supplierProductApi,
    messageApi
}
