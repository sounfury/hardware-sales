package com.hardware.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Message;
import com.hardware.sales.entity.Product;
import com.hardware.sales.mapper.MessageMapper;
import com.hardware.sales.service.MessageService;
import com.hardware.sales.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 站内消息服务测试，验证补货提醒会驱动商品状态流转，并支持供应商回复。
 */
@SpringBootTest
@Transactional
class MessageServiceImplTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ProductService productService;

    @Autowired
    private MessageMapper messageMapper;

    /**
     * 发送补货提醒后，商品应进入补货中状态，并记录当前跟进供应商。
     */
    @Test
    void shouldMarkProductInRestockingWhenSendingRestockNotice() {
        Message request = new Message();
        request.setReceiverId(103L);
        request.setProductId(7L);
        request.setContent("PPR 弯头库存偏低，请尽快安排补货。");

        messageService.sendRestockMessage(request, 1L);

        Product product = productService.getById(7L);
        Message savedMessage = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, 103L)
                .eq(Message::getProductId, 7L)
                .eq(Message::getType, "RESTOCK_NOTICE")
                .orderByDesc(Message::getId)
                .last("limit 1"));

        assertEquals(1, product.getRestockStatus());
        assertEquals(3L, product.getRestockSupplierId());
        assertEquals("RESTOCK_NOTICE", savedMessage.getType());
        assertNull(savedMessage.getReplyToMessageId());
    }

    /**
     * 已经处于补货中的商品不允许重复发起新的补货提醒。
     */
    @Test
    void shouldRejectDuplicateRestockNoticeWhenProductAlreadyRestocking() {
        Message request = new Message();
        request.setReceiverId(101L);
        request.setProductId(1L);
        request.setContent("电钻继续补货提醒");

        BizException exception = assertThrows(BizException.class,
                () -> messageService.sendRestockMessage(request, 1L));

        assertEquals("当前商品已处于补货中，请先完成当前补货", exception.getMessage());
    }

    /**
     * 供应商回复补货提醒时，系统应回写一条关联原提醒的回复消息。
     */
    @Test
    void shouldCreateSupplierReplyForRestockNotice() {
        Message request = new Message();
        request.setReceiverId(103L);
        request.setProductId(7L);
        request.setContent("PPR 弯头库存偏低，请尽快安排补货。");
        messageService.sendRestockMessage(request, 1L);

        Message notice = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, 103L)
                .eq(Message::getProductId, 7L)
                .eq(Message::getType, "RESTOCK_NOTICE")
                .orderByDesc(Message::getId)
                .last("limit 1"));

        Message reply = new Message();
        reply.setReplyToMessageId(notice.getId());
        reply.setContent("已收到，今天 15:00 到店里补货。");

        messageService.sendRestockReply(reply, 103L);

        Message savedReply = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getReplyToMessageId, notice.getId())
                .eq(Message::getType, "RESTOCK_REPLY")
                .orderByDesc(Message::getId)
                .last("limit 1"));

        assertEquals(103L, savedReply.getSenderId());
        assertEquals(1L, savedReply.getReceiverId());
        assertEquals(7L, savedReply.getProductId());
        assertEquals("RESTOCK_REPLY", savedReply.getType());
    }

    /**
     * 管理员确认收货后，商品应恢复为正常状态。
     */
    @Test
    void shouldResetRestockStateWhenCompletingRestock() {
        productService.completeRestock(1L);

        Product product = productService.getById(1L);

        assertEquals(0, product.getRestockStatus());
        assertNull(product.getRestockSupplierId());
    }
}
