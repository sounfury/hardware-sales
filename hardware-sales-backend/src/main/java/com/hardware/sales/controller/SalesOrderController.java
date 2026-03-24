package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.SalesOrder;
import com.hardware.sales.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 销售出货控制器，提供销售单的创建、查询、结算操作
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    /** 分页查询销售单，支持按单号、客户名称、日期范围筛选 */
    @GetMapping("/page")
    public Result<IPage<SalesOrder>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(salesOrderService.pageQuery(pageNum, pageSize, orderNo, customerName, startDate, endDate));
    }

    /** 查询销售单详情（含明细列表） */
    @GetMapping("/{id}")
    public Result<SalesOrder> detail(@PathVariable Long id) {
        return Result.ok(salesOrderService.detail(id));
    }

    /** 创建销售单，自动扣减库存和生成财务记录 */
    @PostMapping
    public Result<?> create(@RequestBody SalesOrder order) {
        salesOrderService.createOrder(order);
        return Result.ok();
    }

    /** 结算销售单，将收款状态标记为已结算 */
    @PutMapping("/settle/{id}")
    public Result<?> settle(@PathVariable Long id) {
        salesOrderService.settle(id);
        return Result.ok();
    }
}
