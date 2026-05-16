const { salesMiniappApi } = require('../../utils/api.js')

Page({
    data: { orders: [], loading: false, finished: false, pageNum: 1, pageSize: 10, total: 0 },

    onShow() {
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().setData({ selected: 1, isCustomer: true })
        }
        this.loadOrders(true)
    },

    onPullDownRefresh() {
        this.loadOrders(true).then(() => wx.stopPullDownRefresh())
    },

    onReachBottom() {
        if (!this.data.finished && !this.data.loading) this.loadOrders(false)
    },

    async loadOrders(reset = false) {
        if (this.data.loading) return
        const pageNum = reset ? 1 : this.data.pageNum
        this.setData({ loading: true })
        try {
            const res = await salesMiniappApi.myPage({ pageNum, pageSize: this.data.pageSize })
            const newList = (res.records || []).map(item => ({
                ...item,
                statusText: item.paymentStatus === 0 ? '待结算' : '已结算',
                statusClass: item.paymentStatus === 0 ? 'status-pending' : 'status-settled'
            }))
            const prevCount = reset ? 0 : this.data.orders.length
            this.setData({
                orders: reset ? newList : [...this.data.orders, ...newList],
                pageNum: pageNum + 1,
                total: res.total,
                finished: prevCount + newList.length >= res.total
            })
        } catch (err) {
            console.error(err)
        } finally {
            this.setData({ loading: false })
        }
    },

    goDetail(e) {
        wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${e.currentTarget.dataset.id}` })
    }
})
