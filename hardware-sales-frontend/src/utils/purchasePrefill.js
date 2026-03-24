/**
 * 获取当前本地日期字符串，避免使用 UTC 日期导致采购日期偏移。
 *
 * @returns {string} YYYY-MM-DD 格式的本地日期
 */
export function getCurrentLocalDate() {
  const currentDate = new Date()
  const year = currentDate.getFullYear()
  const month = String(currentDate.getMonth() + 1).padStart(2, '0')
  const day = String(currentDate.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 构造跳转到采购管理页时使用的预填查询参数。
 * 这里直接传原始 JSON 字符串，交给 vue-router 统一做 URL 编码。
 *
 * @param {Object} options 预填参数
 * @param {number|string|null} [options.supplierId] 供应商 ID
 * @param {string} [options.orderDate] 采购日期
 * @param {Array<{productId:number|string, quantity:number|string}>} [options.items] 采购明细
 * @returns {Object} 可直接用于 router.push 的 query
 */
export function buildPurchasePrefillQuery(options = {}) {
  const {
    supplierId = null,
    orderDate = getCurrentLocalDate(),
    items = [],
  } = options

  const query = {
    openCreate: '1',
    orderDate,
  }

  if (supplierId !== null && supplierId !== undefined && supplierId !== '') {
    query.supplierId = String(supplierId)
  }

  if (Array.isArray(items) && items.length) {
    query.items = JSON.stringify(
      items.map((item) => ({
        productId: Number(item.productId),
        quantity: Number(item.quantity ?? 1),
      })),
    )
  }

  return query
}

/**
 * 读取查询参数中的单个值，兼容 vue-router 可能返回数组的场景。
 *
 * @param {unknown} value 查询参数值
 * @returns {string} 单个字符串值
 */
export function getSingleQueryValue(value) {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }
  return value == null ? '' : String(value)
}

/**
 * 解析采购管理页的采购明细预填参数，兼容旧的 encodeURIComponent 格式。
 *
 * @param {unknown} rawItems 路由上的 items 参数
 * @returns {Array<{productId:number, quantity:number}>} 解析后的采购明细
 */
export function parsePurchasePrefillItems(rawItems) {
  const itemText = getSingleQueryValue(rawItems)
  if (!itemText) return []

  const candidateList = [itemText]

  try {
    const decodedText = decodeURIComponent(itemText)
    if (decodedText !== itemText) {
      candidateList.push(decodedText)
    }
  } catch {
    // 历史 query 可能本来就是未编码字符串，这里忽略解码失败即可。
  }

  for (const candidate of candidateList) {
    try {
      const parsedItems = JSON.parse(candidate)
      if (!Array.isArray(parsedItems)) continue

      return parsedItems
        .map((item) => ({
          productId: Number(item?.productId),
          quantity: Number(item?.quantity ?? 1),
        }))
        .filter((item) => Number.isFinite(item.productId) && item.quantity > 0)
    } catch {
      // 同时兼容新旧格式，某一种解析失败时继续尝试下一种。
    }
  }

  return []
}
