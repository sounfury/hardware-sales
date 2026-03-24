package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.PurchaseOrder;
import com.hardware.sales.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购进货控制器，提供采购单的创建、查询、结算操作
 */
@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    /** 分页查询采购单，支持按单号、供应商、日期范围筛选 */
    @GetMapping("/page")
    public Result<IPage<PurchaseOrder>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(purchaseOrderService.pageQuery(pageNum, pageSize, orderNo, supplierId, startDate, endDate));
    }

    /** 查询采购单详情（含明细列表） */
    @GetMapping("/{id}")
    public Result<PurchaseOrder> detail(@PathVariable Long id) {
        return Result.ok(purchaseOrderService.detail(id));
    }

    /** 创建采购单，自动更新库存和生成财务记录 */
    @PostMapping
    public Result<PurchaseOrder> create(@RequestBody PurchaseOrder order) {
        purchaseOrderService.createOrder(order);
        return Result.ok(order);
    }

    /** 结算采购单，将付款状态标记为已结算 */
    @PutMapping("/settle/{id}")
    public Result<?> settle(@PathVariable Long id) {
        purchaseOrderService.settle(id);
        return Result.ok();
    }
}
