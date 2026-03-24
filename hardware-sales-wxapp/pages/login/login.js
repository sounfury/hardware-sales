const { authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
    data: {
        username: '',
        password: '',
        loading: false
    },

    onLoad() {
        // 若已登录，直接跳走
        if (auth.getToken() && auth.isSupplier()) {
            wx.switchTab({ url: '/pages/quote/quote' })
        }
    },

    async handleLogin() {
        const { username, password } = this.data
        if (!username || !password) {
            wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
            return
        }

        this.setData({ loading: true })
        try {
            const res = await authApi.login({ username, password })

            // 校验角色
            if (res.user.role !== 'SUPPLIER') {
                wx.showToast({ title: '当前账号不可进入供应商端', icon: 'none' })
                this.setData({ loading: false })
                return
            }

            // 保存登录态
            auth.saveLoginInfo(res)

            wx.showToast({ title: '登录成功', icon: 'success' })

            setTimeout(() => {
                wx.switchTab({ url: '/pages/quote/quote' })
            }, 1000)

        } catch (err) {
            this.setData({ loading: false })
        }
    }
})
