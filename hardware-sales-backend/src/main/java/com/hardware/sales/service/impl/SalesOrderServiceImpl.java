package com.hardware.sales.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.*;
import com.hardware.sales.mapper.SalesItemMapper;
import com.hardware.sales.mapper.SalesOrderMapper;
import com.hardware.sales.service.FinanceRecordService;
import com.hardware.sales.service.InventoryLogService;
import com.hardware.sales.service.ProductService;
import com.hardware.sales.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 销售出货服务实现，包含创建销售单（自动出库 + 生成财务记录）和结算
 */
@Service
@RequiredArgsConstructor
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder>
        implements SalesOrderService {

    private final SalesItemMapper salesItemMapper;
    private final ProductService productService;
    private final InventoryLogService inventoryLogService;
    private final FinanceRecordService financeRecordService;

    @Override
    public IPage<SalesOrder> pageQuery(Integer pageNum, Integer pageSize,
                                       String orderNo, String customerName,
                                       String startDate, String endDate) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(orderNo), SalesOrder::getOrderNo, orderNo)
                .like(StrUtil.isNotBlank(customerName), SalesOrder::getCustomerName, customerName)
                .ge(startDate != null, SalesOrder::getOrderDate, startDate)
                .le(endDate != null, SalesOrder::getOrderDate, endDate)
                .orderByDesc(SalesOrder::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    @Override
    public SalesOrder detail(Long id) {
        SalesOrder order = getById(id);
        if (order == null) {
            throw new BizException("销售单不存在");
        }
        // 查询明细，联表获取商品名称
        MPJLambdaWrapper<SalesItem> wrapper = new MPJLambdaWrapper<SalesItem>()
                .selectAll(SalesItem.class)
                .selectAs(Product::getName, SalesItem::getProductName)
                .leftJoin(Product.class, Product::getId, SalesItem::getProductId)
                .eq(SalesItem::getOrderId, id);
        List<SalesItem> items = salesItemMapper.selectJoinList(SalesItem.class, wrapper);
        order.setItems(items);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SalesOrder order) {
        List<SalesItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            throw new BizException("销售明细不能为空");
        }

        // 生成单号
        String orderNo = "SO" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
        order.setOrderNo(orderNo);

        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SalesItem item : items) {
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalAmount(totalAmount);
        order.setPaymentStatus(0);

        // 保存主单
        save(order);

        // 保存明细 + 校验并更新库存 + 记录流水
        for (SalesItem item : items) {
            item.setOrderId(order.getId());
            salesItemMapper.insert(item);

            // 校验库存
            Product product = productService.getById(item.getProductId());
            if (product == null) {
                throw new BizException("商品不存在，ID: " + item.getProductId());
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BizException("商品【" + product.getName() + "】库存不足，当前库存: " + product.getStock());
            }

            int beforeStock = product.getStock();
            int afterStock = beforeStock - item.getQuantity();
            product.setStock(afterStock);
            productService.updateById(product);

            // 记录库存流水
            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setType(2); // 出库
            log.setQuantity(item.getQuantity());
            log.setBeforeStock(beforeStock);
            log.setAfterStock(afterStock);
            log.setRefType("SALES");
            log.setRefOrderId(order.getId());
            inventoryLogService.save(log);
        }

        // 生成收入记录
        FinanceRecord record = new FinanceRecord();
        record.setType(1); // 收入
        record.setAmount(totalAmount);
        record.setRefType("SALES");
        record.setRefOrderId(order.getId());
        record.setPaymentStatus(0);
        record.setRemark("销售单: " + orderNo);
        financeRecordService.save(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(Long id) {
        SalesOrder order = getById(id);
        if (order == null) {
            throw new BizException("销售单不存在");
        }
        if (order.getPaymentStatus() == 1) {
            throw new BizException("该销售单已结算");
        }
        order.setPaymentStatus(1);
        updateById(order);

        // 同步更新收支记录
        financeRecordService.lambdaUpdate()
                .eq(FinanceRecord::getRefType, "SALES")
                .eq(FinanceRecord::getRefOrderId, id)
                .set(FinanceRecord::getPaymentStatus, 1)
                .update();
    }
}
