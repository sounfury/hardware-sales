const { supplierProductApi } = require('../../utils/api.js')

Page({
    data: {
        keyword: '',
        list: [],
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        loading: false
    },

    onShow() {
        this.refreshList()
    },

    handleSearch() {
        this.refreshList()
    },

    async fetchList(isLoadMore = false) {
        if (this.data.loading) return
        this.setData({ loading: true })

        try {
            const { pageNum, pageSize, keyword, list } = this.data
            const params = { pageNum, pageSize }
            if (keyword) params.productName = keyword
            const res = await supplierProductApi.page(params)

            const records = res.records || []
            const hasMore = pageNum < res.pages

            this.setData({
                list: isLoadMore ? [...list, ...records] : records,
                hasMore,
                loading: false
            })
        } catch (err) {
            this.setData({ loading: false })
        }
    },

    refreshList() {
        this.setData({ pageNum: 1, hasMore: true }, () => {
            this.fetchList(false).then(() => {
                wx.stopPullDownRefresh()
            })
        })
    },

    onPullDownRefresh() {
        this.refreshList()
    },

    onReachBottom() {
        if (this.data.hasMore && !this.data.loading) {
            this.setData({ pageNum: this.data.pageNum + 1 }, () => {
                this.fetchList(true)
            })
        }
    },

    goToAdd() {
        wx.navigateTo({ url: '/pages/quote-add/quote-add' })
    },

    goToEdit(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: `/pages/quote-edit/quote-edit?id=${id}` })
    },

    handleDelete(e) {
        const id = e.currentTarget.dataset.id
        wx.showModal({
            title: '提示',
            content: '确定要删除这条报价吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await supplierProductApi.delete(id)
                        wx.showToast({ title: '删除成功', icon: 'success' })
                        this.refreshList()
                    } catch (err) {
                        // 已在 request 拦截处理错入并提示
                    }
                }
            }
        })
    }
})
