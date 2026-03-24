package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.Message;
import com.hardware.sales.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 站内消息控制器，提供消息的发送、查询、已读标记
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 分页查询消息列表，支持按接收人、已读状态和消息类型筛选。 */
    @GetMapping("/page")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<IPage<Message>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long receiverId,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) String type) {
        validateCurrentReceiver(receiverId);
        return Result.ok(messageService.pageQuery(pageNum, pageSize, receiverId, isRead, type));
    }

    /** 发送一条普通消息。 */
    @PostMapping
    @SaCheckRole("ADMIN")
    public Result<?> send(@RequestBody Message message) {
        messageService.sendGeneralMessage(message, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 发送一条补货提醒消息。 */
    @PostMapping("/restock")
    @SaCheckRole("ADMIN")
    public Result<?> sendRestock(@RequestBody Message message) {
        messageService.sendRestockMessage(message, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 供应商回复补货提醒。 */
    @PostMapping("/restock-reply")
    @SaCheckRole("SUPPLIER")
    public Result<?> sendRestockReply(@RequestBody Message message) {
        messageService.sendRestockReply(message, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 标记单条消息为已读 */
    @PutMapping("/read/{id}")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<?> markRead(@PathVariable Long id) {
        validateMessageOwnership(id);
        messageService.markRead(id);
        return Result.ok();
    }

    /** 标记指定接收人的全部消息为已读 */
    @PutMapping("/read-all")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<?> markAllRead(@RequestParam Long receiverId) {
        validateCurrentReceiver(receiverId);
        messageService.markAllRead(receiverId);
        return Result.ok();
    }

    /** 查询指定接收人的未读消息数 */
    @GetMapping("/unread-count")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<Long> unreadCount(@RequestParam Long receiverId) {
        validateCurrentReceiver(receiverId);
        return Result.ok(messageService.unreadCount(receiverId));
    }

    private void validateCurrentReceiver(Long receiverId) {
        if (!Objects.equals(receiverId, StpUtil.getLoginIdAsLong())) {
            throw new BizException("仅支持查询当前登录用户的消息");
        }
    }

    private void validateMessageOwnership(Long id) {
        Message message = messageService.getById(id);
        if (message == null) {
            throw new BizException("消息不存在");
        }
        if (!Objects.equals(message.getReceiverId(), StpUtil.getLoginIdAsLong())) {
            throw new BizException("仅支持处理当前登录用户的消息");
        }
    }
}
