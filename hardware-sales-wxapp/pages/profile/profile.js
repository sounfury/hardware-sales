const { supplierApi, authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

/** 构造供应商资料表单的默认值。 */
function buildFormData(profile, user) {
    return {
        companyName: profile?.companyName || '',
        contactPerson: profile?.contactPerson || user?.nickname || '',
        contactPhone: profile?.contactPhone || user?.phone || '',
        address: profile?.address || '',
        businessScope: profile?.businessScope || ''
    }
}

Page({
    data: {
        hasProfile: false,
        profile: null,
        formData: buildFormData(null, null),
        statusClass: '',
        statusIcon: '',
        statusTitle: '',
        submitting: false
    },

    /** 页面展示时刷新当前用户与供应商资料。 */
    onShow() {
        this.loadProfile()
    },

    /** 加载当前登录用户的供应商申请信息。 */
    async loadProfile() {
        const user = await this.refreshCurrentUser()
        if (!user) {
            return
        }

        try {
            const supplierInfo = await supplierApi.current()
            if (supplierInfo) {
                auth.saveSupplierInfo(supplierInfo)
                this.setData({
                    hasProfile: true,
                    profile: supplierInfo,
                    formData: buildFormData(supplierInfo, user)
                })
                this.updateStatusBanner(supplierInfo.auditStatus)
                return
            }

            auth.clearSupplierInfo()
            this.setData({
                hasProfile: false,
                profile: null,
                formData: buildFormData(null, user)
            })
            this.updateStatusBanner(null)
        } catch (err) {
            console.error(err)
        }
    },

    /** 刷新当前登录用户信息，保证审核通过后本地角色能及时更新。 */
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

    /** 根据审核状态刷新顶部提示条。 */
    updateStatusBanner(status) {
        let statusClass = ''
        let statusIcon = ''
        let statusTitle = ''
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

    /** 同步表单输入内容。 */
    handleInput(e) {
        const field = e.currentTarget.dataset.field
        this.setData({
            [`formData.${field}`]: e.detail.value
        })
    },

    /** 提交供应商资料申请或修改。 */
    async handleSubmit() {
        const { formData, hasProfile, profile } = this.data
        if (!formData.companyName || !formData.contactPerson || !formData.contactPhone) {
            wx.showToast({ title: '请填写带*的必填项', icon: 'none' })
            return
        }

        this.setData({ submitting: true })
        try {
            if (hasProfile && profile && profile.id) {
                await supplierApi.update({
                    ...formData,
                    id: profile.id
                })
                wx.showToast({ title: '修改成功', icon: 'success' })
            } else {
                await supplierApi.create(formData)
                wx.showToast({ title: '提交成功', icon: 'success' })
            }

            await this.loadProfile()
        } catch (err) {
            // 错误提示已在请求层处理
        } finally {
            this.setData({ submitting: false })
        }
    },

    /** 退出当前登录账号。 */
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
