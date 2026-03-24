import request from '@/utils/request'

/** 分页查询消息 */
export function getMessagePage(params) {
  return request.get('/message/page', { params })
}

/** 发送消息 */
export function sendMessage(data) {
  return request.post('/message', data)
}

/** 发送补货提醒 */
export function sendRestockMessage(data) {
  return request.post('/message/restock', data)
}

/** 标记单条已读 */
export function markMessageRead(id) {
  return request.put(`/message/read/${id}`)
}

/** 标记全部已读 */
export function markAllMessageRead(receiverId) {
  return request.put('/message/read-all', null, { params: { receiverId } })
}

/** 查询未读数量 */
export function getUnreadCount(receiverId) {
  return request.get('/message/unread-count', { params: { receiverId } })
}
