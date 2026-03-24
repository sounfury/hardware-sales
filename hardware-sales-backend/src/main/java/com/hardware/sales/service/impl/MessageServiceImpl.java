package com.hardware.sales.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Message;
import com.hardware.sales.entity.Product;
import com.hardware.sales.entity.Supplier;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.mapper.MessageMapper;
import com.hardware.sales.service.MessageService;
import com.hardware.sales.service.ProductService;
import com.hardware.sales.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内消息服务实现，负责普通消息和补货提醒的统一落库。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    private final ProductService productService;
    private final SupplierService supplierService;

    /** 分页查询消息，并补充发送人昵称与商品名称。 */
    @Override
    public IPage<Message> pageQuery(Integer pageNum, Integer pageSize, Long receiverId, Integer isRead, String type) {
        MPJLambdaWrapper<Message> wrapper = new MPJLambdaWrapper<Message>()
                .selectAll(Message.class)
                .selectAs(SysUser::getNickname, Message::getSenderName)
                .selectAs(Product::getName, Message::getProductName)
                .leftJoin(SysUser.class, SysUser::getId, Message::getSenderId)
                .leftJoin(Product.class, Product::getId, Message::getProductId)
                .eq(receiverId != null, Message::getReceiverId, receiverId)
                .eq(isRead != null, Message::getIsRead, isRead)
                .eq(StrUtil.isNotBlank(type), Message::getType, type)
                .orderByDesc(Message::getCreateTime);
        IPage<Message> page = baseMapper.selectJoinPage(new Page<>(pageNum, pageSize), Message.class, wrapper);
        fillReplyTitles(page.getRecords());
        return page;
    }

    /** 标记单条消息为已读。 */
    @Override
    public void markRead(Long id) {
        lambdaUpdate().eq(Message::getId, id).set(Message::getIsRead, 1).update();
    }

    /** 标记指定接收人的全部未读消息为已读。 */
    @Override
    public void markAllRead(Long receiverId) {
        lambdaUpdate()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1)
                .update();
    }

    /** 查询指定接收人的未读消息数量。 */
    @Override
    public long unreadCount(Long receiverId) {
        return lambdaQuery()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getIsRead, 0)
                .count();
    }

    /** 发送普通站内消息。 */
    @Override
    public void sendGeneralMessage(Message message, Long senderId) {
        if (message.getReceiverId() == null) {
            throw new BizException("接收人不能为空");
        }
        if (StrUtil.isBlank(message.getTitle())) {
            throw new BizException("消息标题不能为空");
        }
        if (StrUtil.isBlank(message.getContent())) {
            throw new BizException("消息内容不能为空");
        }

        Message entity = new Message();
        entity.setSenderId(senderId);
        entity.setReceiverId(resolveApprovedSupplierUserId(message.getReceiverId()).getUserId());
        entity.setType("GENERAL");
        entity.setProductId(null);
        entity.setReplyToMessageId(null);
        entity.setTitle(message.getTitle());
        entity.setContent(message.getContent());
        entity.setIsRead(0);
        save(entity);
    }

    /** 发送与商品关联的补货提醒消息。 */
    @Override
    @Transactional
    public void sendRestockMessage(Message message, Long senderId) {
        if (message.getReceiverId() == null) {
            throw new BizException("接收人不能为空");
        }
        if (message.getProductId() == null) {
            throw new BizException("商品不能为空");
        }
        Product product = productService.getById(message.getProductId());
        if (product == null) {
            throw new BizException("商品不存在");
        }
        Supplier supplier = resolveApprovedSupplierUserId(message.getReceiverId());

        productService.markRestocking(product.getId(), supplier.getId());

        Message entity = new Message();
        entity.setSenderId(senderId);
        entity.setReceiverId(supplier.getUserId());
        entity.setType("RESTOCK_NOTICE");
        entity.setProductId(product.getId());
        entity.setTitle(product.getName() + " 补货提醒");
        entity.setContent(StrUtil.blankToDefault(message.getContent(), "当前商品库存偏低，请尽快安排补货。"));
        entity.setReplyToMessageId(null);
        entity.setIsRead(0);
        save(entity);
    }

    /**
     * 供应商回复补货提醒时，系统回写一条关联原提醒的消息。
     */
    @Override
    @Transactional
    public void sendRestockReply(Message message, Long senderId) {
        if (message.getReplyToMessageId() == null) {
            throw new BizException("原补货提醒不能为空");
        }
        if (StrUtil.isBlank(message.getContent())) {
            throw new BizException("回复内容不能为空");
        }
        resolveApprovedSupplierUserId(senderId);

        Message notice = getById(message.getReplyToMessageId());
        if (notice == null) {
            throw new BizException("原补货提醒不存在");
        }
        if (!"RESTOCK_NOTICE".equals(notice.getType())) {
            throw new BizException("仅支持回复补货提醒消息");
        }
        if (!Objects.equals(notice.getReceiverId(), senderId)) {
            throw new BizException("仅补货提醒接收方可回复");
        }

        Product product = notice.getProductId() == null ? null : productService.getById(notice.getProductId());
        Message entity = new Message();
        entity.setSenderId(senderId);
        entity.setReceiverId(notice.getSenderId());
        entity.setType("RESTOCK_REPLY");
        entity.setProductId(notice.getProductId());
        entity.setReplyToMessageId(notice.getId());
        entity.setTitle((product != null ? product.getName() : "补货提醒") + " 补货回复");
        entity.setContent(message.getContent());
        entity.setIsRead(0);
        save(entity);
    }

    /**
     * 当前消息模型的供应商接收人仍然是 `sys_user.id`，这里统一校验该用户是否属于已审核供应商。
     */
    private Supplier resolveApprovedSupplierUserId(Long receiverId) {
        Supplier supplier = supplierService.lambdaQuery()
                .eq(Supplier::getUserId, receiverId)
                .one();
        if (supplier == null) {
            throw new BizException("接收供应商不存在");
        }
        if (supplier.getAuditStatus() == null || supplier.getAuditStatus() != 1) {
            throw new BizException("仅支持向已审核通过的供应商发送消息");
        }

        return supplier;
    }

    private void fillReplyTitles(List<Message> records) {
        List<Long> replyIds = records.stream()
                .map(Message::getReplyToMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (replyIds.isEmpty()) {
            return;
        }
        Map<Long, Message> replyMessageMap = lambdaQuery()
                .in(Message::getId, replyIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Message::getId, Function.identity()));
        records.forEach(record -> {
            if (record.getReplyToMessageId() == null) {
                return;
            }
            Message replyTarget = replyMessageMap.get(record.getReplyToMessageId());
            if (replyTarget != null) {
                record.setReplyToTitle(replyTarget.getTitle());
            }
        });
    }
}
