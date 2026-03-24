package com.hardware.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.PurchaseItem;
import com.hardware.sales.entity.PurchaseOrder;
import com.hardware.sales.mapper.PurchaseItemMapper;
import com.hardware.sales.service.PurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 采购单服务测试，验证采购单会强制复用供应商商品报价关系。
 */
@SpringBootTest
@Transactional
class PurchaseOrderServiceImplTest {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private PurchaseItemMapper purchaseItemMapper;

    /**
     * 即使前端传入了其他价格，系统也应以供应商商品里维护的供货价为准。
     */
    @Test
    void shouldUseSupplierQuotedPriceWhenCreatingPurchaseOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(1L);
        order.setOrderDate(LocalDate.of(2026, 3, 24));
        order.setRemark("测试供应商报价复用");

        PurchaseItem item = new PurchaseItem();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("999.99"));
        order.setItems(List.of(item));

        purchaseOrderService.createOrder(order);

        PurchaseOrder savedOrder = purchaseOrderService.getById(order.getId());
        PurchaseItem savedItem = purchaseItemMapper.selectOne(new LambdaQueryWrapper<PurchaseItem>()
                .eq(PurchaseItem::getOrderId, order.getId()));

        assertEquals(0, new BigDecimal("286.00").compareTo(savedItem.getPrice()));
        assertEquals(0, new BigDecimal("572.00").compareTo(savedOrder.getTotalAmount()));
    }

    /**
     * 来自“完成补货”流程的采购单会在创建后自动结算，避免后台残留未结算状态。
     */
    @Test
    void shouldAutoSettleWhenCreatingAutoSettledPurchaseOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(1L);
        order.setOrderDate(LocalDate.of(2026, 3, 24));
        order.setAutoSettle(true);

        PurchaseItem item = new PurchaseItem();
        item.setProductId(1L);
        item.setQuantity(1);
        order.setItems(List.of(item));

        purchaseOrderService.createOrder(order);

        PurchaseOrder savedOrder = purchaseOrderService.getById(order.getId());

        assertEquals(1, savedOrder.getPaymentStatus());
    }

    /**
     * 如果供应商没有维护某商品的供货关系，采购单必须被拒绝。
     */
    @Test
    void shouldRejectProductsOutsideSupplierCatalog() {
        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(1L);
        order.setOrderDate(LocalDate.of(2026, 3, 24));

        PurchaseItem item = new PurchaseItem();
        item.setProductId(4L);
        item.setQuantity(1);
        order.setItems(List.of(item));

        BizException exception = assertThrows(BizException.class, () -> purchaseOrderService.createOrder(order));

        assertEquals("所选供应商未维护该商品的供货信息，请先在供应商商品中维护", exception.getMessage());
    }
}
