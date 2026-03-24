const { supplierProductApi } = require('../../utils/api.js')

Page({
    data: {
        id: null,
        quote: null,
        formData: {
            supplyPrice: '',
            remark: ''
        },
        submitting: false
    },

    onLoad(options) {
        if (options.id) {
            this.setData({ id: options.id })
            this.loadQuoteDetail(options.id)
        } else {
            wx.showToast({ title: '参数错误', icon: 'none' })
            setTimeout(() => wx.navigateBack(), 1500)
        }
    },

    async loadQuoteDetail(id) {
        try {
            const res = await supplierProductApi.getById(id)
            this.setData({
                quote: res,
                formData: {
                    supplyPrice: res.supplyPrice,
                    remark: res.remark || ''
                }
            })
        } catch (err) {
            setTimeout(() => wx.navigateBack(), 1500)
        }
    },

    handleInput(e) {
        const field = e.currentTarget.dataset.field
        this.setData({
            [`formData.${field}`]: e.detail.value
        })
    },

    async handleSubmit() {
        const { id, formData, quote } = this.data
        if (!formData.supplyPrice || isNaN(Number(formData.supplyPrice)) || Number(formData.supplyPrice) < 0) {
            wx.showToast({ title: '请输入正确的供货单价', icon: 'none' })
            return
        }

        this.setData({ submitting: true })
        try {
            await supplierProductApi.update({
                id,
                supplierId: quote.supplierId,
                productId: quote.productId,
                supplyPrice: Number(formData.supplyPrice),
                remark: formData.remark
            })

            wx.showToast({ title: '修改成功', icon: 'success' })

            setTimeout(() => {
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
