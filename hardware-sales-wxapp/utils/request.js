const auth = require('./auth.js')

const BASE_URL = 'http://192.168.0.103:8080'
/**
 * 统一网络请求封装
 */
function request(options) {
    return new Promise((resolve, reject) => {
        const token = auth.getToken()
        const header = {
            'Content-Type': 'application/json',
            ...options.header
        }

        if (token) {
            header['Authorization'] = token
        }

        wx.request({
            url: BASE_URL + options.url,
            method: options.method || 'GET',
            data: options.data,
            header: header,
            timeout: 10000,
            success: (res) => {
                const { statusCode, data } = res
                if (statusCode === 200) {
                    if (data.code === 200) {
                        resolve(data.data)
                    } else if (data.code === 401) {
                        auth.clearLoginInfo()
                        wx.showToast({ title: '登录已过期', icon: 'none' })
                        setTimeout(() => {
                            wx.reLaunch({ url: '/pages/login/login' })
                        }, 1500)
                        reject(new Error(data.msg || 'Unauthorized'))
                    } else if (data.msg && data.msg.includes('档案不存在')) {
                        wx.showModal({
                            title: '提示',
                            content: '您还没完善供应商资料，请先填写资料提交审核。',
                            confirmText: '去填写',
                            showCancel: false,
                            success(modalRes) {
                                if (modalRes.confirm) {
                                    wx.switchTab({ url: '/pages/profile/profile' })
                                }
                            }
                        })
                        reject(new Error(data.msg))
                    } else if (data.msg && data.msg.includes('尚未审核通过')) {
                        wx.showModal({
                            title: '提示',
                            content: '您的供应商资料正在等待管理员审核，请耐心等待。',
                            showCancel: false
                        })
                        reject(new Error(data.msg))
                    } else {
                        wx.showToast({ title: data.msg || '操作失败', icon: 'none' })
                        reject(new Error(data.msg || 'Error'))
                    }
                } else if (statusCode === 401) {
                    auth.clearLoginInfo()
                    wx.showToast({ title: '登录已过期', icon: 'none' })
                    setTimeout(() => {
                        wx.reLaunch({ url: '/pages/login/login' })
                    }, 1500)
                    reject(new Error('Unauthorized'))
                } else {
                    wx.showToast({ title: '网络请求失败', icon: 'none' })
                    reject(new Error(`HTTP Error: ${statusCode}`))
                }
            },
            fail: (err) => {
                wx.showToast({ title: '网络连接异常', icon: 'none' })
                reject(err)
            }
        })
    })
}

module.exports = {
    request,
    BASE_URL
}
