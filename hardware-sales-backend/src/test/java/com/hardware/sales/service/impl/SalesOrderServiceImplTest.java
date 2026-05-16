package com.hardware.sales.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Product;
import com.hardware.sales.entity.SalesItem;
import com.hardware.sales.entity.SalesOrder;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.service.ProductService;
import com.hardware.sales.service.SalesOrderService;
import com.hardware.sales.service.SysUserService;
import com.hardware.sales.service.dto.MiniappPreorderItemRequest;
import com.hardware.sales.service.dto.MiniappPreorderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 销售单服务测试，覆盖小程序客户预定场景。
 */
@SpringBootTest
@Transactional
class SalesOrderServiceImplTest {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 客户预定时应强制使用商品当前售价，并生成 MINIAPP 来源销售单。
     */
    @Test
    void shouldCreateMiniappPreorderUsingCurrentSalePrice() {
        Long customerUserId = createCustomer("sales_case_customer_01");
        Product product = productService.getById(1L);
        int beforeStock = product.getStock();

        MiniappPreorderItemRequest itemRequest = new MiniappPreorderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        MiniappPreorderRequest request = new MiniappPreorderRequest();
        request.setRemark("到店自提");
        request.setItems(List.of(itemRequest));

        SalesOrder order = salesOrderService.createMiniappPreorder(customerUserId, request);
        SalesOrder savedOrder = salesOrderService.detail(order.getId());
        Product savedProduct = productService.getById(1L);
        SalesItem savedItem = savedOrder.getItems().get(0);

        assertEquals("MINIAPP", savedOrder.getOrderSource());
        assertEquals(customerUserId, savedOrder.getCustomerUserId());
        assertEquals(0, savedOrder.getPaymentStatus());
        assertEquals(0, product.getSalePrice().compareTo(savedItem.getPrice()));
        assertEquals(0, new BigDecimal("998.00").compareTo(savedOrder.getTotalAmount()));
        assertEquals(beforeStock - 2, savedProduct.getStock());
    }

    /**
     * 客户只能查看自己的预定详情，不能越权查看其他客户订单。
     */
    @Test
    void shouldRejectViewingOtherCustomerOrder() {
        Long ownerUserId = createCustomer("sales_case_customer_02");
        Long otherUserId = createCustomer("sales_case_customer_03");

        MiniappPreorderItemRequest itemRequest = new MiniappPreorderItemRequest();
        itemRequest.setProductId(4L);
        itemRequest.setQuantity(1);

        MiniappPreorderRequest request = new MiniappPreorderRequest();
        request.setItems(List.of(itemRequest));

        SalesOrder order = salesOrderService.createMiniappPreorder(ownerUserId, request);
        BizException exception = assertThrows(BizException.class,
                () -> salesOrderService.detailForCustomer(order.getId(), otherUserId));

        assertEquals("无权查看该预定单", exception.getMessage());
    }

    /**
     * 我的预定分页只应返回当前客户自己的订单。
     */
    @Test
    void shouldQueryOrdersByCustomer() {
        Long customerUserId = createCustomer("sales_case_customer_04");

        MiniappPreorderItemRequest itemRequest = new MiniappPreorderItemRequest();
        itemRequest.setProductId(5L);
        itemRequest.setQuantity(2);

        MiniappPreorderRequest request = new MiniappPreorderRequest();
        request.setItems(List.of(itemRequest));
        salesOrderService.createMiniappPreorder(customerUserId, request);

        IPage<SalesOrder> page = salesOrderService.pageQueryByCustomer(customerUserId, 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals(customerUserId, page.getRecords().get(0).getCustomerUserId());
    }

    /**
     * 创建测试客户，避免依赖外部演示数据。
     */
    private Long createCustomer(String username) {
        SysUser customer = new SysUser();
        customer.setUsername(username);
        customer.setPassword("admin123");
        customer.setNickname(username);
        customer.setPhone("1370000" + String.format("%04d", Math.abs(username.hashCode()) % 10000));
        customer.setRole("CUSTOMER");
        customer.setStatus(1);
        sysUserService.createUser(customer);
        return customer.getId();
    }
}
