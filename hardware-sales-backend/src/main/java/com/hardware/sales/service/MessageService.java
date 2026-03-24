package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.Message;

/**
 * 站内消息服务接口
 */
public interface MessageService extends IService<Message> {

    /** 分页查询消息，支持按接收人、已读状态和类型筛选 */
    IPage<Message> pageQuery(Integer pageNum, Integer pageSize, Long receiverId, Integer isRead, String type);

    /** 标记单条消息为已读 */
    void markRead(Long id);

    /** 标记指定接收人的全部消息为已读 */
    void markAllRead(Long receiverId);

    /** 查询指定接收人的未读消息数 */
    long unreadCount(Long receiverId);

    /** 发送普通站内消息 */
    void sendGeneralMessage(Message message, Long senderId);

    /** 发送补货提醒消息 */
    void sendRestockMessage(Message message, Long senderId);

    /** 供应商回复补货提醒 */
    void sendRestockReply(Message message, Long senderId);
}
