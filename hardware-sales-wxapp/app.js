// app.js
const auth = require('./utils/auth')

App({
  onLaunch() {
    // 检查登录态
    if (!auth.getToken() || !auth.isSupplier()) {
      // 可以在此处统一重定向到登录页，但需要确保页面路径已经注册
      // 这里我们在页面级别的 onLoad 再做判断，防止与 app.json 的 redirect 冲突
    }
  },
  globalData: {
    userInfo: null,
    supplierInfo: null
  }
})
