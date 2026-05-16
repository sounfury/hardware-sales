const { salesMiniappApi } = require('../../utils/api.js')

Page({
    data: { order: null, loading: true },

    onLoad(options) {
        if (!options.id) { wx.navigateBack(); return }
        this.loadOrder(options.id)
    },

    async loadOrder(id) {
        this.setData({ loading: true })
        try {
            const order = await salesMiniappApi.getById(id)
            this.setData({
                order: {
                    ...order,
                    statusText: order.paymentStatus === 0 ? '待结算' : '已结算',
                    statusClass: order.paymentStatus === 0 ? 'status-pending' : 'status-settled'
                }
            })
        } catch (err) { wx.navigateBack() }
        finally { this.setData({ loading: false }) }
    }
})
