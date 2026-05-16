const { authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
    data: { user: null },

    onShow() {
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().setData({ selected: 2, isCustomer: true })
        }
        this.loadUser()
    },

    async loadUser() {
        this.setData({ user: auth.getUser() })
        try {
            const user = await authApi.info()
            if (user) { auth.updateUser(user); this.setData({ user }) }
        } catch (err) {}
    },

    goOrders() {
        wx.switchTab({ url: '/pages/my-orders/my-orders' })
    },

    handleLogout() {
        wx.showModal({
            title: '提示', content: '确定要退出登录吗？',
            success: async (res) => {
                if (res.confirm) {
                    try { await authApi.logout() } catch (e) {}
                    auth.clearLoginInfo()
                    wx.reLaunch({ url: '/pages/login/login' })
                }
            }
        })
    }
})
