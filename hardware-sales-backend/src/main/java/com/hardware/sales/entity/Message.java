package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息实体，支持普通消息、补货提醒和补货回复。
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String type;
    private Long productId;
    /** 回复关联的原消息ID */
    private Long replyToMessageId;
    private String title;
    private String content;
    /** 0-未读 1-已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 发送人昵称（非数据库字段） */
    @TableField(exist = false)
    private String senderName;

    /** 关联商品名称（非数据库字段） */
    @TableField(exist = false)
    private String productName;

    /** 被回复消息的标题（非数据库字段） */
    @TableField(exist = false)
    private String replyToTitle;
}
