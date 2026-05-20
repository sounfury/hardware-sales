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
        this._syncRole()
    },

    /** 每次宿主页面切换到前台时重新同步角色与高亮状态。 */
    pageLifetimes: {
        show() {
            this._syncRole()
            this._syncSelected()
        }
    },

    methods: {
        /** 同步当前登录角色到 isCustomer。 */
        _syncRole() {
            this.setData({ isCustomer: auth.isCustomer() })
        },

        /** 根据当前页面路径同步高亮 tab。 */
        _syncSelected() {
            const pages = getCurrentPages()
            if (!pages.length) return
            const currentPath = '/' + pages[pages.length - 1].route
            const tabs = auth.isCustomer() ? this.data.customerTabs : this.data.supplierTabs
            const idx = tabs.findIndex(t => t.pagePath === currentPath)
            if (idx !== -1) {
                this.setData({ selected: idx })
            }
        },

        /** 切换 tab 并更新高亮。 */
        switchTab(e) {
            const url = e.currentTarget.dataset.url
            const tabs = auth.isCustomer() ? this.data.customerTabs : this.data.supplierTabs
            const idx = tabs.findIndex(t => t.pagePath === url)
            if (idx !== -1) {
                this.setData({ selected: idx })
            }
            wx.switchTab({ url })
        }
    }
})
