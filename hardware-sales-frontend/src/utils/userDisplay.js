/**
 * 统一获取前端展示用用户名，避免数据库里已损坏的昵称直接显示为一串问号。
 */
export function getDisplayName(user, fallback = '业务管理员') {
  const nickname = normalizeDisplayText(user?.nickname)
  if (nickname) {
    return nickname
  }

  const username = normalizeDisplayText(user?.username)
  if (username) {
    return username
  }

  return fallback
}

/**
 * 过滤空值和纯问号昵称，防止乱码占位符污染界面展示。
 */
function normalizeDisplayText(value) {
  if (typeof value !== 'string') {
    return ''
  }

  const trimmedValue = value.trim()
  if (!trimmedValue) {
    return ''
  }

  if (/^\?+$/.test(trimmedValue) || /^？+$/.test(trimmedValue)) {
    return ''
  }

  return trimmedValue
}
