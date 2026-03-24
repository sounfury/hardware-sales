const { messageApi } = require('../../utils/api.js')
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

    onShow() {
        this.refreshList()
        this.fetchUnreadCount()
    },

    async fetchUnreadCount() {
        const user = auth.getUser()
        if (!user) return
        try {
            const count = await messageApi.getUnreadCount(user.id)
            this.setData({ unreadCount: typeof count === 'number' ? count : (count.data || 0) })

            // 更新 TabBar badge
            if (this.data.unreadCount > 0) {
                wx.setTabBarBadge({ index: 1, text: String(this.data.unreadCount) })
            } else {
                wx.removeTabBarBadge({ index: 1 })
            }
        } catch (err) { }
    },

    async fetchList(isLoadMore = false) {
        if (this.data.loading) return
        this.setData({ loading: true })

        const user = auth.getUser()
        if (!user) return

        try {
            const { pageNum, pageSize, list } = this.data
            const res = await messageApi.page({
                pageNum,
                pageSize,
                receiverId: user.id
            })

            const records = (res.records || []).map(item => {
                // format time
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

    refreshList() {
        this.setData({ pageNum: 1, hasMore: true }, () => {
            this.fetchList(false).then(() => {
                wx.stopPullDownRefresh()
            })
        })
    },

    onPullDownRefresh() {
        this.refreshList()
        this.fetchUnreadCount()
    },

    onReachBottom() {
        if (this.data.hasMore && !this.data.loading) {
            this.setData({ pageNum: this.data.pageNum + 1 }, () => {
                this.fetchList(true)
            })
        }
    },

    async handleMsgClick(e) {
        const item = e.currentTarget.dataset.item

        // 标记已读
        if (!item.isRead) {
            try {
                await messageApi.markRead(item.id)
                // 本地更新状态
                const newList = this.data.list.map(m => m.id === item.id ? { ...m, isRead: 1 } : m)
                this.setData({ list: newList })
                this.fetchUnreadCount()
            } catch (err) { }
        }

        // 补货提醒弹窗
        if (item.type === 'RESTOCK_NOTICE') {
            this.setData({
                showReplyModal: true,
                currentMsg: item,
                replyContent: ''
            })
        }
    },

    async handleReadAll() {
        if (this.data.unreadCount === 0) return
        const user = auth.getUser()
        if (!user) return

        try {
            await messageApi.markAllRead(user.id)
            wx.showToast({ title: '已全部标记为已读', icon: 'success' })
            this.refreshList()
            this.fetchUnreadCount()
        } catch (err) { }
    },

    closeReplyModal() {
        this.setData({ showReplyModal: false, currentMsg: null })
    },

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
