import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

function clearAuthState(store) {
  store.token = ''
  store.userInfo = null
  localStorage.removeItem('token')
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
  },

  actions: {
    /** 登录 */
    async login(username, password) {
      const res = await loginApi({ username, password })
      if (res.data.user?.role !== 'ADMIN') {
        clearAuthState(this)
        throw new Error('当前后台仅允许业务管理员登录')
      }
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
      this.userInfo = res.data.user
    },

    /** 获取用户信息 */
    async fetchUserInfo() {
      const res = await getUserInfo()
      if (res.data?.role !== 'ADMIN') {
        clearAuthState(this)
        throw new Error('当前后台仅允许业务管理员登录')
      }
      this.userInfo = res.data
      return res.data
    },

    /** 退出 */
    async logout() {
      try {
        await logoutApi()
      } finally {
        clearAuthState(this)
      }
    },
  },
})
