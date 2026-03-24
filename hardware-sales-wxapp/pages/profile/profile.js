const { supplierApi, authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
    data: {
        hasProfile: false,
        profile: null,
        formData: {
            companyName: '',
            contactPerson: '',
            contactPhone: '',
            address: '',
            businessScope: ''
        },
        statusClass: '',
        statusIcon: '',
        statusTitle: '',
        submitting: false
    },

    onShow() {
        this.loadProfile()
    },

    async loadProfile() {
        const user = auth.getUser()
        if (!user) return

        try {
            let supplierInfo = auth.getSupplierInfo()
            if (supplierInfo && supplierInfo.id) {
                // try to refresh
                try {
                    const res = await supplierApi.getById(supplierInfo.id)
                    if (res) {
                        supplierInfo = res
                        auth.saveSupplierInfo(supplierInfo)
                    }
                } catch (e) { }
            }

            if (supplierInfo) {
                this.setData({
                    hasProfile: true,
                    profile: supplierInfo,
                    formData: {
                        companyName: supplierInfo.companyName || '',
                        contactPerson: supplierInfo.contactPerson || '',
                        contactPhone: supplierInfo.contactPhone || '',
                        address: supplierInfo.address || '',
                        businessScope: supplierInfo.businessScope || ''
                    }
                })
                this.updateStatusBanner(supplierInfo.auditStatus)
            }
        } catch (err) {
            console.error(err)
        }
    },

    updateStatusBanner(status) {
        let statusClass = '', statusIcon = '', statusTitle = ''
        if (status === 0) {
            statusClass = 'pending'
            statusIcon = '⏳'
            statusTitle = '等待管理员审核'
        } else if (status === 1) {
            statusClass = 'approved'
            statusIcon = '✅'
            statusTitle = '审核通过'
        } else if (status === 2) {
            statusClass = 'rejected'
            statusIcon = '❌'
            statusTitle = '审核被驳回'
        }
        this.setData({ statusClass, statusIcon, statusTitle })
    },

    handleInput(e) {
        const field = e.currentTarget.dataset.field
        this.setData({
            [`formData.${field}`]: e.detail.value
        })
    },

    async handleSubmit() {
        const { formData, hasProfile, profile } = this.data
        if (!formData.companyName || !formData.contactPerson || !formData.contactPhone) {
            wx.showToast({ title: '请填写带*的必填项', icon: 'none' })
            return
        }

        this.setData({ submitting: true })
        try {
            let res
            if (hasProfile && profile && profile.id) {
                res = await supplierApi.update({
                    ...formData,
                    id: profile.id
                })
                wx.showToast({ title: '修改成功', icon: 'success' })
            } else {
                res = await supplierApi.create(formData)
                wx.showToast({ title: '提交成功', icon: 'success' })
            }

            const newInfo = Object.assign({}, profile || {}, formData)
            if (res && res.id) newInfo.id = res.id
            else if (res && typeof res === 'number') newInfo.id = res // incase backend returns id directly
            newInfo.auditStatus = newInfo.auditStatus ?? 0

            auth.saveSupplierInfo(newInfo)

            this.setData({
                submitting: false,
                hasProfile: true,
                profile: newInfo
            })
            this.updateStatusBanner(newInfo.auditStatus)

        } catch (err) {
            this.setData({ submitting: false })
        }
    },

    handleLogout() {
        wx.showModal({
            title: '提示',
            content: '确定要退出登录吗？',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        await authApi.logout()
                    } catch (e) { }
                    auth.clearLoginInfo()
                    wx.reLaunch({ url: '/pages/login/login' })
                }
            }
        })
    }
})
