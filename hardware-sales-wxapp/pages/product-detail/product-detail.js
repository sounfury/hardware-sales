const { productApi } = require('../../utils/api.js')
const cart = require('../../utils/cart.js')

Page({
    data: {
        product: null,
        loading: true,
        quantity: 1,
        totalAmount: '0.00',
        cartRemark: '',
        cartTotalCount: 0,
        cartTotalAmount: '0.00'
    },

    /** 页面初始化时根据商品 ID 拉取详情。 */
    onLoad(options) {
        if (!options.id) { wx.navigateBack(); return }
        this.loadProduct(options.id)
    },

    /** 页面重新展示时刷新共享购物车摘要。 */
    onShow() {
        this.refreshCartState()
    },

    /** 加载商品详情并同步页面标题。 */
    async loadProduct(id) {
        this.setData({ loading: true })
        try {
            const product = await productApi.getById(id)
            wx.setNavigationBarTitle({ title: product.name })
            this.setData({ product })
            this.updateTotal()
        } catch (err) { wx.navigateBack() }
        finally { this.setData({ loading: false }) }
    },

    /** 刷新共享购物车的备注和汇总信息。 */
    refreshCartState() {
        const cartState = cart.getCartState()
        const summary = cart.getCartSummary(cartState)
        this.setData({
            cartRemark: cartState.remark,
            ...summary
        })
    },

    /** 同步计算展示金额。 */
    updateTotal() {
        const { product, quantity } = this.data
        if (!product) return
        const total = (product.salePrice * quantity).toFixed(2)
        this.setData({ totalAmount: total })
    },

    /** 将当前商品待加入数量减 1。 */
    decreaseQty() {
        if (this.data.quantity <= 1) return
        this.setData({ quantity: this.data.quantity - 1 })
        this.updateTotal()
    },

    /** 将当前商品待加入数量加 1。 */
    increaseQty() {
        const stock = this.data.product ? this.data.product.stock : 999
        if (this.data.quantity >= stock) { wx.showToast({ title: '已达库存上限', icon: 'none' }); return }
        this.setData({ quantity: this.data.quantity + 1 })
        this.updateTotal()
    },

    /** 手动输入待加入数量时保持最小值为 1。 */
    onQtyInput(e) {
        this.setData({ quantity: Math.max(1, parseInt(e.detail.value) || 1) })
        this.updateTotal()
    },

    /** 维护整单备注草稿，返回列表页后可直接统一提交。 */
    onCartRemarkInput(e) {
        cart.updateCartRemark(e.detail.value)
        this.refreshCartState()
    },

    /** 将当前商品按选中数量加入购物车。 */
    addCurrentProductToCart() {
        const { product, quantity } = this.data
        if (!product) return
        const cartState = cart.getCartState()
        const existingItem = cartState.items.find((item) => item.id === product.id)
        const nextQuantity = (existingItem ? existingItem.quantity : 0) + quantity
        if (nextQuantity > product.stock) {
            wx.showToast({ title: '购买数量超过库存', icon: 'none' })
            return
        }
        cart.setCartItemQuantity(product, nextQuantity)
        this.refreshCartState()
        wx.showToast({ title: '已加入清单', icon: 'success' })
    },

    /** 回到列表页继续加购或统一提交。 */
    goBackToProductList() {
        wx.navigateBack()
    }
})
