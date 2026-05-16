const auth = require('../utils/auth.js')

Component({
    data: {
        /** 客户端 tab 列表。 */
        customerTabs: [
            { pagePath: '/pages/product-list/product-list', text: '商品', icon: '🛍️' },
            { pagePath: '/pages/my-orders/my-orders', text: '我的预定', icon: '📋' },
            { pagePath: '/pages/customer-profile/customer-profile', text: '我的', icon: '👤' }
        ],
        /** 供应商端 tab 列表。 */
        supplierTabs: [
            { pagePath: '/pages/quote/quote', text: '我的报价', icon: '📦' },
            { pagePath: '/pages/message/message', text: '消息中心', icon: '💬' },
            { pagePath: '/pages/profile/profile', text: '我的', icon: '👤' }
        ],
        selected: 0,
        isCustomer: false
    },

    attached() {
        this.setData({ isCustomer: auth.isCustomer() })
    },

    methods: {
        /** 切换 tab。 */
        switchTab(e) {
            const url = e.currentTarget.dataset.url
            wx.switchTab({ url })
        }
    }
})
