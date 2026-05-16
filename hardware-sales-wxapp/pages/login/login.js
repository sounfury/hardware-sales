const { authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
    data: {
        mode: 'login',
        registerType: 'CUSTOMER', // 注册类型：CUSTOMER / SUPPLIER
        username: '',
        password: '',
        confirmPassword: '',
        nickname: '',
        phone: '',
        loading: false
    },

    /** 页面加载时，如果已有登录态则直接进入默认页。 */
    onLoad() {
        if (auth.getToken()) {
            wx.switchTab({ url: auth.getDefaultHomePage() })
        }
    },

    /** 切换登录/注册模式。 */
    switchMode(e) {
        const mode = e.currentTarget.dataset.mode
        if (!mode || mode === this.data.mode) return
        this.setData({ mode, loading: false })
    },

    /** 切换注册类型（客户 / 供应商）。 */
    switchRegisterType(e) {
        const registerType = e.currentTarget.dataset.type
        if (registerType === this.data.registerType) return
        this.setData({ registerType })
    },

    /** 同步输入框内容。 */
    handleInput(e) {
        const field = e.currentTarget.dataset.field
        this.setData({ [field]: e.detail.value })
    },

    /** 统一处理表单提交。 */
    async handleSubmit() {
        if (this.data.mode === 'register') {
            await this.handleRegister()
            return
        }
        await this.handleLogin()
    },

    /** 执行登录。 */
    async handleLogin() {
        const { username, password } = this.data
        if (!username || !password) {
            wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
            return
        }
        this.setData({ loading: true })
        try {
            const res = await authApi.login({ username, password })
            auth.saveLoginInfo(res)
            wx.showToast({ title: '登录成功', icon: 'success' })
            setTimeout(() => {
                wx.switchTab({ url: auth.getDefaultHomePage() })
            }, 1000)
        } catch (err) {
            // 错误提示已在请求层处理
        } finally {
            this.setData({ loading: false })
        }
    },

    /** 执行注册。 */
    async handleRegister() {
        const { username, password, confirmPassword, nickname, phone, registerType } = this.data
        if (!username || !password) {
            wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
            return
        }
        if (password !== confirmPassword) {
            wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
            return
        }
        this.setData({ loading: true })
        try {
            const res = await authApi.register({ registerType, username, password, nickname, phone })
            auth.saveLoginInfo(res)
            wx.showToast({ title: '注册成功', icon: 'success' })
            setTimeout(() => {
                // 客户注册后直接进商品列表；供应商进资料完善页
                const target = registerType === 'CUSTOMER'
                    ? '/pages/product-list/product-list'
                    : '/pages/profile/profile'
                wx.switchTab({ url: target })
            }, 1000)
        } catch (err) {
            // 错误提示已在请求层处理
        } finally {
            this.setData({ loading: false })
        }
    }
})
