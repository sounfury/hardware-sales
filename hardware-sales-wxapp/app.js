// app.js
const auth = require('./utils/auth')

App({
  /** 小程序启动时仅保留全局数据初始化，具体登录跳转放到页面中处理。 */
  onLaunch() {
    if (!auth.getToken()) {
      return
    }
  },
  globalData: {
    userInfo: null,
    supplierInfo: null
  }
})
