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
import com.hardware.sales.service.SysUserService;
import com.hardware.sales.service.dto.MiniappPreorderItemRequest;
import com.hardware.sales.service.dto.MiniappPreorderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final SysUserService sysUserService;

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
        return getOrderWithItems(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SalesOrder order) {
        List<SalesItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            throw new BizException("销售明细不能为空");
        }
        if (order.getOrderDate() == null) {
            throw new BizException("销售日期不能为空");
        }
        order.setOrderSource(StrUtil.blankToDefault(order.getOrderSource(), "MANUAL"));

        saveSalesOrder(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SalesOrder createMiniappPreorder(Long customerUserId, MiniappPreorderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException("预定商品不能为空");
        }

        SysUser customer = sysUserService.getById(customerUserId);
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            throw new BizException("当前登录用户不是客户");
        }

        SalesOrder order = new SalesOrder();
        order.setCustomerUserId(customerUserId);
        order.setCustomerName(StrUtil.blankToDefault(customer.getNickname(), customer.getUsername()));
        order.setCustomerPhone(customer.getPhone());
        order.setOrderSource("MINIAPP");
        order.setOrderDate(LocalDate.now());
        order.setRemark(StrUtil.trim(request.getRemark()));

        List<SalesItem> items = buildMiniappSalesItems(request.getItems());
        saveSalesOrder(order, items);
        order.setItems(items);
        return order;
    }

    @Override
    public IPage<SalesOrder> pageQueryByCustomer(Long customerUserId, Integer pageNum, Integer pageSize) {
        return lambdaQuery()
                .eq(SalesOrder::getCustomerUserId, customerUserId)
                .orderByDesc(SalesOrder::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    @Override
    public SalesOrder detailForCustomer(Long id, Long customerUserId) {
        SalesOrder order = getOrderWithItems(id);
        if (!customerUserId.equals(order.getCustomerUserId())) {
            throw new BizException("无权查看该预定单");
        }
        return order;
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

    /**
     * 保存销售单主表、明细、库存流水和财务记录。
     */
    private void saveSalesOrder(SalesOrder order, List<SalesItem> items) {
        validateSalesItems(items);

        String orderNo = "SO" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
        order.setOrderNo(orderNo);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SalesItem item : items) {
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalAmount(totalAmount);
        order.setPaymentStatus(0);
        save(order);

        // 这里按明细逐个校验库存并扣减，确保订单、库存流水、财务记录一次事务内完成。
        for (SalesItem item : items) {
            item.setOrderId(order.getId());
            salesItemMapper.insert(item);

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

            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setType(2);
            log.setQuantity(item.getQuantity());
            log.setBeforeStock(beforeStock);
            log.setAfterStock(afterStock);
            log.setRefType("SALES");
            log.setRefOrderId(order.getId());
            inventoryLogService.save(log);
        }

        FinanceRecord record = new FinanceRecord();
        record.setType(1);
        record.setAmount(totalAmount);
        record.setRefType("SALES");
        record.setRefOrderId(order.getId());
        record.setPaymentStatus(0);
        record.setRemark("销售单: " + orderNo);
        financeRecordService.save(record);
    }

    /**
     * 将小程序预定请求转换为销售明细，并强制使用商品当前售价。
     */
    private List<SalesItem> buildMiniappSalesItems(List<MiniappPreorderItemRequest> requestItems) {
        List<SalesItem> items = new ArrayList<>();
        for (MiniappPreorderItemRequest requestItem : requestItems) {
            if (requestItem == null || requestItem.getProductId() == null || requestItem.getQuantity() == null) {
                throw new BizException("预定商品参数不完整");
            }

            Product product = productService.getById(requestItem.getProductId());
            if (product == null) {
                throw new BizException("商品不存在，ID: " + requestItem.getProductId());
            }
            if (product.getSalePrice() == null) {
                throw new BizException("商品【" + product.getName() + "】尚未配置售价");
            }

            SalesItem item = new SalesItem();
            item.setProductId(requestItem.getProductId());
            item.setQuantity(requestItem.getQuantity());
            item.setPrice(product.getSalePrice());
            items.add(item);
        }
        return items;
    }

    /**
     * 校验销售明细基础字段，避免空商品或非法数量落单。
     */
    private void validateSalesItems(List<SalesItem> items) {
        for (SalesItem item : items) {
            if (item == null || item.getProductId() == null) {
                throw new BizException("销售明细商品不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException("商品数量必须大于 0");
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("商品价格不能为空且不能小于 0");
            }
        }
    }

    /**
     * 查询销售单并加载商品明细名称。
     */
    private SalesOrder getOrderWithItems(Long id) {
        SalesOrder order = getById(id);
        if (order == null) {
            throw new BizException("销售单不存在");
        }

        MPJLambdaWrapper<SalesItem> wrapper = new MPJLambdaWrapper<SalesItem>()
                .selectAll(SalesItem.class)
                .selectAs(Product::getName, SalesItem::getProductName)
                .leftJoin(Product.class, Product::getId, SalesItem::getProductId)
                .eq(SalesItem::getOrderId, id);
        List<SalesItem> items = salesItemMapper.selectJoinList(SalesItem.class, wrapper);
        order.setItems(items);
        return order;
    }
}
