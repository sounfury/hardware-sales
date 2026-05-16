const { productApi, salesMiniappApi } = require('../../utils/api.js')
const auth = require('../../utils/auth.js')
const cart = require('../../utils/cart.js')

Page({
    data: {
        products: [],
        searchName: '',
        loading: false,
        finished: false,
        pageNum: 1,
        pageSize: 10,
        total: 0,
        cartItems: [],
        cartRemark: '',
        cartTypeCount: 0,
        cartTotalCount: 0,
        cartTotalAmount: '0.00',
        cartPreviewText: '',
        hasCart: false,
        submittingCart: false
    },

    /** 页面初始化时校验客户身份并拉取商品、购物车数据。 */
    onLoad() {
        if (!auth.isCustomer()) {
            wx.reLaunch({ url: '/pages/login/login' })
            return
        }
        this.refreshCartState()
        this.loadProducts(true)
    },

    /** 页面重新展示时同步 tab 高亮、购物车状态和最新商品数据。 */
    onShow() {
        if (typeof this.getTabBar === 'function' && this.getTabBar()) {
            this.getTabBar().setData({ selected: 0, isCustomer: true })
        }
        if (auth.isCustomer()) {
            this.refreshCartState()
            this.loadProducts(true)
        }
    },

    /** 下拉时重新加载第一页商品数据。 */
    onPullDownRefresh() {
        this.loadProducts(true).then(() => wx.stopPullDownRefresh())
    },

    /** 触底时继续加载下一页商品。 */
    onReachBottom() {
        if (!this.data.finished && !this.data.loading) {
            this.loadProducts(false)
        }
    },

    /** 加载商品列表，重置时从第一页重新查询。 */
    async loadProducts(reset = false) {
        if (this.data.loading) return
        const pageNum = reset ? 1 : this.data.pageNum
        this.setData({ loading: true })
        try {
            const res = await productApi.page({
                pageNum,
                pageSize: this.data.pageSize,
                name: this.data.searchName
            })
            const newList = this.decorateProductsWithCart(res.records || [])
            const prevCount = reset ? 0 : this.data.products.length
            this.setData({
                products: reset ? newList : [...this.data.products, ...newList],
                pageNum: pageNum + 1,
                total: res.total,
                finished: prevCount + newList.length >= res.total
            })
        } catch (err) {
            console.error(err)
        } finally {
            this.setData({ loading: false })
        }
    },

    /** 同步搜索框输入内容。 */
    onSearchInput(e) {
        this.setData({ searchName: e.detail.value })
    },

    /** 按关键字重新查询商品列表。 */
    onSearch() {
        this.loadProducts(true)
    },

    /** 将购物车中的已选数量同步到商品列表展示。 */
    decorateProductsWithCart(products) {
        const cartQuantityMap = new Map(this.data.cartItems.map((item) => [item.id, item.quantity]))
        return products.map((product) => ({
            ...product,
            cartQuantity: cartQuantityMap.get(product.id) || 0
        }))
    },

    /** 读取本地购物车并刷新当前页汇总信息。 */
    refreshCartState() {
        const cartState = cart.getCartState()
        const summary = cart.getCartSummary(cartState)
        const cartPreviewText = this.buildCartPreviewText(cartState.items)
        this.setData({
            cartItems: cartState.items,
            cartRemark: cartState.remark,
            cartPreviewText,
            hasCart: summary.cartTotalCount > 0,
            ...summary
        })
        if (this.data.products.length > 0) {
            this.setData({
                products: this.decorateProductsWithCart(this.data.products)
            })
        }
    },

    /** 生成购物车摘要文案，便于用户快速确认已选商品。 */
    buildCartPreviewText(cartItems) {
        if (!cartItems.length) {
            return ''
        }
        const previewText = cartItems
                .slice(0, 3)
                .map((item) => `${item.name} x${item.quantity}`)
                .join('、')
        return cartItems.length > 3 ? `${previewText} 等 ${cartItems.length} 种商品` : previewText
    },

    /** 打开商品详情页，详情页用于补充查看介绍和维护整单备注。 */
    goDetail(e) {
        const id = e.currentTarget.dataset.id
        wx.navigateTo({ url: `/pages/product-detail/product-detail?id=${id}` })
    },

    /** 空函数用于阻止卡片点击冒泡到详情页。 */
    stopPropagation() {},

    /** 将商品加入清单，默认加入 1 件。 */
    addToCart(e) {
        const product = e.currentTarget.dataset.product
        if (product.stock <= 0) {
            wx.showToast({ title: '当前库存不足', icon: 'none' })
            return
        }
        cart.changeCartItemQuantity(product, 1)
        this.refreshCartState()
    },

    /** 增加某个商品在清单中的数量。 */
    increaseCartQty(e) {
        const product = e.currentTarget.dataset.product
        const quantity = Number(e.currentTarget.dataset.quantity) || 0
        if (quantity >= product.stock) {
            wx.showToast({ title: '已达库存上限', icon: 'none' })
            return
        }
        cart.changeCartItemQuantity(product, 1)
        this.refreshCartState()
    },

    /** 减少某个商品在清单中的数量，减到 0 时自动移除。 */
    decreaseCartQty(e) {
        const product = e.currentTarget.dataset.product
        cart.changeCartItemQuantity(product, -1)
        this.refreshCartState()
    },

    /** 同步更新整单备注草稿。 */
    onCartRemarkInput(e) {
        const cartState = cart.updateCartRemark(e.detail.value)
        const summary = cart.getCartSummary(cartState)
        this.setData({
            cartRemark: cartState.remark,
            ...summary
        })
    },

    /** 清空购物车并刷新商品列表上的已选数量。 */
    clearCart() {
        if (!this.data.cartTotalCount) {
            return
        }
        wx.showModal({
            title: '提示',
            content: '确定清空当前清单吗？',
            success: (res) => {
                if (!res.confirm) {
                    return
                }
                cart.clearCart()
                this.refreshCartState()
            }
        })
    },

    /** 将购物车中的多个商品一次性提交为一张销售单。 */
    async submitCartOrder() {
        if (!this.data.cartItems.length) {
            wx.showToast({ title: '请先加入商品', icon: 'none' })
            return
        }
        this.setData({ submittingCart: true })
        try {
            await salesMiniappApi.preorder({
                items: this.data.cartItems.map((item) => ({
                    productId: item.id,
                    quantity: item.quantity
                })),
                remark: this.data.cartRemark || undefined
            })
            wx.showToast({ title: '预定成功！', icon: 'success' })
            cart.clearCart()
            this.refreshCartState()
            setTimeout(() => {
                this.loadProducts(true)
                wx.switchTab({ url: '/pages/my-orders/my-orders' })
            }, 800)
        } catch (err) {
            // 错误已在请求层弹出
        } finally {
            this.setData({ submittingCart: false })
        }
    }
})
