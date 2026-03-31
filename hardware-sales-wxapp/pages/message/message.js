const { messageApi, authApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')

Page({
    data: {
        list: [],
        pageNum: 1,
        pageSize: 10,
        hasMore: true,
        loading: false,
        unreadCount: 0,

        showReplyModal: false,
        currentMsg: null,
        replyContent: '',
        replying: false
    },

    /** 页面展示时校验供应商身份并加载消息列表。 */
    async onShow() {
        const canUseMessage = await this.ensureSupplierAccess()
        if (!canUseMessage) {
            this.setData({
                list: [],
                unreadCount: 0,
                hasMore: false,
                loading: false
            })
            wx.removeTabBarBadge({ index: 1 })
            return
        }
        this.refreshList()
        this.fetchUnreadCount()
    },

    /** 登录用户若未成为供应商，则引导去资料页完成申请。 */
    async ensureSupplierAccess() {
        const user = await this.refreshCurrentUser()
        if (!user) {
            wx.reLaunch({ url: '/pages/login/login' })
            return false
        }
        if (auth.isSupplier()) {
            return true
        }
        wx.showModal({
            title: '提示',
            content: '审核通过后才能查看和回复补货消息，请先在“我的”页面完善资料并等待管理员审核。',
            showCancel: false,
            success: () => {
                wx.switchTab({ url: '/pages/profile/profile' })
            }
        })
        return false
    },

    /** 刷新当前登录用户信息，避免审核通过后仍使用旧角色。 */
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

    /** 拉取未读消息数量并同步到 TabBar。 */
    async fetchUnreadCount() {
        const user = auth.getUser()
        if (!user) {
            return
        }
        try {
            const count = await messageApi.getUnreadCount(user.id)
            this.setData({ unreadCount: typeof count === 'number' ? count : (count.data || 0) })

            if (this.data.unreadCount > 0) {
                wx.setTabBarBadge({ index: 1, text: String(this.data.unreadCount) })
            } else {
                wx.removeTabBarBadge({ index: 1 })
            }
        } catch (err) { }
    },

    /** 分页拉取站内消息列表。 */
    async fetchList(isLoadMore = false) {
        if (this.data.loading) {
            return
        }
        this.setData({ loading: true })

        const user = auth.getUser()
        if (!user) {
            this.setData({ loading: false })
            return
        }

        try {
            const { pageNum, pageSize, list } = this.data
            const res = await messageApi.page({
                pageNum,
                pageSize,
                receiverId: user.id
            })

            const records = (res.records || []).map(item => {
                // 统一裁剪到分钟，避免列表里时间展示过长。
                item.createTimeShort = item.createTime ? item.createTime.substring(0, 16) : ''
                return item
            })
            const hasMore = pageNum < res.pages

            this.setData({
                list: isLoadMore ? [...list, ...records] : records,
                hasMore,
                loading: false
            })
        } catch (err) {
            this.setData({ loading: false })
        }
    },

    /** 重置分页并重新加载消息。 */
    refreshList() {
        this.setData({ pageNum: 1, hasMore: true }, () => {
            this.fetchList(false).then(() => {
                wx.stopPullDownRefresh()
            })
        })
    },

    /** 处理下拉刷新。 */
    onPullDownRefresh() {
        this.refreshList()
        this.fetchUnreadCount()
    },

    /** 处理触底加载更多。 */
    onReachBottom() {
        if (this.data.hasMore && !this.data.loading) {
            this.setData({ pageNum: this.data.pageNum + 1 }, () => {
                this.fetchList(true)
            })
        }
    },

    /** 点击消息后处理已读逻辑与补货回复弹窗。 */
    async handleMsgClick(e) {
        const item = e.currentTarget.dataset.item

        // 首次打开消息时先回写已读状态，保证未读数同步正确。
        if (!item.isRead) {
            try {
                await messageApi.markRead(item.id)
                const newList = this.data.list.map(m => m.id === item.id ? { ...m, isRead: 1 } : m)
                this.setData({ list: newList })
                this.fetchUnreadCount()
            } catch (err) { }
        }

        // 只有补货提醒需要供应商在小程序内直接回复。
        if (item.type === 'RESTOCK_NOTICE') {
            this.setData({
                showReplyModal: true,
                currentMsg: item,
                replyContent: ''
            })
        }
    },

    /** 一键将当前用户的全部消息标记为已读。 */
    async handleReadAll() {
        if (this.data.unreadCount === 0) {
            return
        }
        const user = auth.getUser()
        if (!user) {
            return
        }

        try {
            await messageApi.markAllRead(user.id)
            wx.showToast({ title: '已全部标记为已读', icon: 'success' })
            this.refreshList()
            this.fetchUnreadCount()
        } catch (err) { }
    },

    /** 关闭回复弹窗。 */
    closeReplyModal() {
        this.setData({ showReplyModal: false, currentMsg: null })
    },

    /** 提交补货回复内容。 */
    async submitReply() {
        const { currentMsg, replyContent } = this.data
        if (!replyContent.trim()) {
            wx.showToast({ title: '请输入回复内容', icon: 'none' })
            return
        }

        this.setData({ replying: true })
        try {
            await messageApi.restockReply({
                replyToMessageId: currentMsg.id,
                content: replyContent.trim()
            })
            wx.showToast({ title: '回复成功', icon: 'success' })
            this.setData({ showReplyModal: false, replying: false, currentMsg: null })
        } catch (err) {
            this.setData({ replying: false })
        }
    }
})
