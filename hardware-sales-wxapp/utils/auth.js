const AUTH_KEY = 'HW_AUTH_INFO'
const SUPPLIER_KEY = 'HW_SUPPLIER_INFO'

/** 保存登录信息。 */
function saveLoginInfo(info) {
    wx.setStorageSync(AUTH_KEY, info)
}

/** 获取完整登录信息。 */
function getLoginInfo() {
    return wx.getStorageSync(AUTH_KEY) || null
}

/** 获取当前 token。 */
function getToken() {
    const info = getLoginInfo()
    return info ? info.token : null
}

/** 获取当前登录用户。 */
function getUser() {
    const info = getLoginInfo()
    return info ? info.user : null
}

/** 判断当前用户是否为审核通过后的供应商角色。 */
function isSupplier() {
    const user = getUser()
    return user && user.role === 'SUPPLIER'
}

/** 更新本地缓存中的用户信息。 */
function updateUser(user) {
    const info = getLoginInfo()
    if (!info) {
        return
    }
    saveLoginInfo({
        ...info,
        user
    })
}

/** 获取登录成功后的默认落地页。 */
function getDefaultHomePage() {
    return isSupplier() ? '/pages/quote/quote' : '/pages/profile/profile'
}

/** 清理全部登录态缓存。 */
function clearLoginInfo() {
    wx.removeStorageSync(AUTH_KEY)
    wx.removeStorageSync(SUPPLIER_KEY)
}

/** 保存供应商资料缓存。 */
function saveSupplierInfo(info) {
    if (!info) {
        clearSupplierInfo()
        return
    }
    wx.setStorageSync(SUPPLIER_KEY, info)
}

/** 获取供应商资料缓存。 */
function getSupplierInfo() {
    return wx.getStorageSync(SUPPLIER_KEY) || null
}

/** 清理供应商资料缓存。 */
function clearSupplierInfo() {
    wx.removeStorageSync(SUPPLIER_KEY)
}

module.exports = {
    saveLoginInfo,
    getLoginInfo,
    getToken,
    getUser,
    isSupplier,
    updateUser,
    getDefaultHomePage,
    clearLoginInfo,
    saveSupplierInfo,
    getSupplierInfo,
    clearSupplierInfo
}
