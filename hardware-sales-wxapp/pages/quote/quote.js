const { supplierProductApi, authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
    data: {
        keyword: '',
        list: [],
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        loading: false
    },

    /** 页面展示时先校验供应商身份，再加载报价数据。 */
    async onShow() {
        const canUseQuote = await this.ensureSupplierAccess()
        if (!canUseQuote) {
            this.setData({
                list: [],
                hasMore: false,
                loading: false
            })
            return
        }
        this.refreshList()
    },

    /** 登录用户若未成为供应商，则引导去资料页完成申请。 */
    async ensureSupplierAccess() {
        const user = await this.refreshCurrentUser()
        if (!user) {
            wx.reLaunch({ url: '/pages/login/login' })
            return false
        }
        if (auth.isSupplier()) {
            return true
        }
        wx.showModal({
            title: '提示',
            content: '审核通过后才能维护报价，请先在“我的”页面完善资料并等待管理员审核。',
            showCancel: false,
            success: () => {
                wx.switchTab({ url: '/pages/profile/profile' })
            }
        })
        return false
    },

    /** 刷新当前登录用户信息，避免审核通过后还保留旧角色。 */
    async refreshCurrentUser() {
        try {
            const user = await authApi.info()
            if (user) {
                auth.updateUser(user)
            }
            return user || auth.getUser()
        } catch (err) {
            return auth.getUser()
        }
    },

    /** 根据关键字重新查询报价列表。 */
    handleSearch() {
        this.refreshList()
    },

    /** 分页拉取当前供应商的报价数据。 */
    async fetchList(isLoadMore = false) {
        if (this.data.loading) {
            return
        }
        this.setData({ loading: true })

        try {
            const user = auth.getUser()
            if (!user) {
                this.setData({ loading: false })
                return
            }

            const { pageNum, pageSize, keyword, list } = this.data
            const params = { pageNum, pageSize }
            if (keyword) {
                params.productName = keyword
            }
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

    /** 重置分页并重新加载列表。 */
    refreshList() {
        this.setData({ pageNum: 1, hasMore: true }, () => {
            this.fetchList(false).then(() => {
                wx.stopPullDownRefresh()
            })
        })
    },

    /** 处理下拉刷新。 */
    onPullDownRefresh() {
        this.refreshList()
    },

    /** 处理触底加载更多。 */
    onReachBottom() {
        if (this.data.hasMore && !this.data.loading) {
            this.setData({ pageNum: this.data.pageNum + 1 }, () => {
                this.fetchList(true)
            })
        }
    },

    /** 跳转到新增报价页。 */
    goToAdd() {
        wx.navigateTo({ url: '/pages/quote-add/quote-add' })
    },

    /** 跳转到报价编辑页。 */
    goToEdit(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: `/pages/quote-edit/quote-edit?id=${id}` })
    },

    /** 删除当前报价记录。 */
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
                        // 已在 request 拦截中统一提示
                    }
                }
            }
        })
    }
})
