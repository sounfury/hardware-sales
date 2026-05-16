const CART_KEY = 'HW_MINIAPP_CART'

/**
 * 读取购物车原始缓存，并在缺失时返回默认结构。
 */
function getCartState() {
    const cartState = wx.getStorageSync(CART_KEY) || {}
    return {
        items: Array.isArray(cartState.items) ? cartState.items : [],
        remark: typeof cartState.remark === 'string' ? cartState.remark : ''
    }
}

/**
 * 将购物车状态写回本地缓存。
 */
function saveCartState(cartState) {
    wx.setStorageSync(CART_KEY, {
        items: Array.isArray(cartState.items) ? cartState.items : [],
        remark: typeof cartState.remark === 'string' ? cartState.remark : ''
    })
}

/**
 * 将商品对象裁剪为购物车需要的最小字段，避免缓存冗余页面数据。
 */
function pickCartProduct(product) {
    return {
        id: product.id,
        name: product.name,
        unit: product.unit,
        salePrice: product.salePrice,
        stock: product.stock,
        brand: product.brand,
        spec: product.spec
    }
}

/**
 * 按商品 ID 更新购物车数量，为 0 时自动从购物车移除。
 */
function setCartItemQuantity(product, quantity) {
    const cartState = getCartState()
    const nextQuantity = Math.max(0, Number(quantity) || 0)
    const filteredItems = cartState.items.filter((item) => item.id !== product.id)

    if (nextQuantity > 0) {
        filteredItems.push({
            ...pickCartProduct(product),
            quantity: Math.min(nextQuantity, product.stock || nextQuantity)
        })
    }

    const nextCartState = {
        ...cartState,
        items: filteredItems
    }
    saveCartState(nextCartState)
    return nextCartState
}

/**
 * 在现有数量基础上增减购物车数量。
 */
function changeCartItemQuantity(product, delta) {
    const cartState = getCartState()
    const currentItem = cartState.items.find((item) => item.id === product.id)
    const currentQuantity = currentItem ? currentItem.quantity : 0
    return setCartItemQuantity(product, currentQuantity + delta)
}

/**
 * 更新整单备注草稿，供列表页和详情页共享。
 */
function updateCartRemark(remark) {
    const cartState = getCartState()
    const nextCartState = {
        ...cartState,
        remark: typeof remark === 'string' ? remark : ''
    }
    saveCartState(nextCartState)
    return nextCartState
}

/**
 * 清空购物车和整单备注。
 */
function clearCart() {
    wx.removeStorageSync(CART_KEY)
}

/**
 * 计算购物车总种类、总件数与总金额，供页面展示汇总信息。
 */
function getCartSummary(cartState = getCartState()) {
    const summary = {
        cartTypeCount: cartState.items.length,
        cartTotalCount: 0,
        cartTotalAmount: '0.00'
    }

    const totalAmount = cartState.items.reduce((amount, item) => {
        summary.cartTotalCount += item.quantity
        return amount + Number(item.salePrice || 0) * item.quantity
    }, 0)

    summary.cartTotalAmount = totalAmount.toFixed(2)
    return summary
}

module.exports = {
    getCartState,
    saveCartState,
    setCartItemQuantity,
    changeCartItemQuantity,
    updateCartRemark,
    clearCart,
    getCartSummary
}
