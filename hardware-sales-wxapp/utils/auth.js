const AUTH_KEY = 'HW_AUTH_INFO'
const SUPPLIER_KEY = 'HW_SUPPLIER_INFO'

function saveLoginInfo(info) {
    wx.setStorageSync(AUTH_KEY, info)
}

function getLoginInfo() {
    return wx.getStorageSync(AUTH_KEY) || null
}

function getToken() {
    const info = getLoginInfo()
    return info ? info.token : null
}

function getUser() {
    const info = getLoginInfo()
    return info ? info.user : null
}

function isSupplier() {
    const user = getUser()
    return user && user.role === 'SUPPLIER'
}

function clearLoginInfo() {
    wx.removeStorageSync(AUTH_KEY)
    wx.removeStorageSync(SUPPLIER_KEY)
}

function saveSupplierInfo(info) {
    wx.setStorageSync(SUPPLIER_KEY, info)
}

function getSupplierInfo() {
    return wx.getStorageSync(SUPPLIER_KEY) || null
}

module.exports = {
    saveLoginInfo,
    getLoginInfo,
    getToken,
    getUser,
    isSupplier,
    clearLoginInfo,
    saveSupplierInfo,
    getSupplierInfo
}
