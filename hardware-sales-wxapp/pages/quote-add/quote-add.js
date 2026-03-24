const { productApi, supplierProductApi } = require('../../utils/api.js')

Page({
    data: {
        step: 1,
        keyword: '',
        productList: [],
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        loading: false,

        selectedProduct: null,
        formData: {
            supplyPrice: '',
            remark: ''
        },
        submitting: false
    },

    onLoad() {
        this.refreshList()
    },

    handleSearch() {
        this.refreshList()
    },

    refreshList() {
        this.setData({ pageNum: 1, hasMore: true }, () => {
            this.fetchList(false)
        })
    },

    async fetchList(isLoadMore = false) {
        if (this.data.loading) return
        this.setData({ loading: true })

        try {
            const { pageNum, pageSize, keyword, productList } = this.data
            const res = await productApi.page({
                pageNum,
                pageSize,
                ...(keyword ? { name: keyword } : {})
            })

            const records = res.records || []
            const hasMore = pageNum < res.pages

            this.setData({
                productList: isLoadMore ? [...productList, ...records] : records,
                hasMore,
                loading: false
            })
        } catch (err) {
            this.setData({ loading: false })
        }
    },

    onReachBottom() {
        if (this.data.step === 1 && this.data.hasMore && !this.data.loading) {
            this.setData({ pageNum: this.data.pageNum + 1 }, () => {
                this.fetchList(true)
            })
        }
    },

    selectProduct(e) {
        const item = e.currentTarget.dataset.item
        this.setData({
            step: 2,
            selectedProduct: item,
            formData: {
                supplyPrice: '',
                remark: ''
            }
        })
        wx.pageScrollTo({ scrollTop: 0, duration: 300 })
    },

    goBack() {
        this.setData({ step: 1 })
    },

    handleInput(e) {
        const field = e.currentTarget.dataset.field
        this.setData({
            [`formData.${field}`]: e.detail.value
        })
    },

    async handleSubmit() {
        const { selectedProduct, formData } = this.data
        if (!formData.supplyPrice || isNaN(Number(formData.supplyPrice)) || Number(formData.supplyPrice) < 0) {
            wx.showToast({ title: '请输入正确的供货单价', icon: 'none' })
            return
        }

        this.setData({ submitting: true })
        try {
            await supplierProductApi.create({
                productId: selectedProduct.id,
                supplyPrice: Number(formData.supplyPrice),
                remark: formData.remark
            })

            wx.showToast({ title: '添加成功', icon: 'success' })

            setTimeout(() => {
                // 通知上一级刷新并返回
                const pages = getCurrentPages()
                if (pages.length > 1) {
                    const prePage = pages[pages.length - 2]
                    if (prePage.refreshList) {
                        prePage.refreshList()
                    }
                }
                wx.navigateBack()
            }, 1500)
        } catch (err) {
            this.setData({ submitting: false })
        }
    }
})
