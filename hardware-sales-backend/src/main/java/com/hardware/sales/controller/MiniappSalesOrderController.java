package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.SalesOrder;
import com.hardware.sales.service.SalesOrderService;
import com.hardware.sales.service.dto.MiniappPreorderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小程序客户销售单控制器，提供立即预定和我的预定查询能力。
 */
@RestController
@RequestMapping("/api/sales/miniapp")
@RequiredArgsConstructor
@SaCheckRole("CUSTOMER")
public class MiniappSalesOrderController {

    private final SalesOrderService salesOrderService;

    /**
     * 客户提交预定，后端自动生成未结算销售单。
     */
    @PostMapping("/preorder")
    public Result<Map<String, Object>> preorder(@RequestBody MiniappPreorderRequest request) {
        SalesOrder order = salesOrderService.createMiniappPreorder(StpUtil.getLoginIdAsLong(), request);
        return Result.ok(Map.of(
                "id", order.getId(),
                "orderNo", order.getOrderNo(),
                "paymentStatus", order.getPaymentStatus()
        ));
    }

    /**
     * 分页查询当前客户自己的预定记录。
     */
    @GetMapping("/my-page")
    public Result<IPage<SalesOrder>> myPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(salesOrderService.pageQueryByCustomer(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    /**
     * 查询当前客户自己的预定详情。
     */
    @GetMapping("/{id}")
    public Result<SalesOrder> detail(@PathVariable Long id) {
        return Result.ok(salesOrderService.detailForCustomer(id, StpUtil.getLoginIdAsLong()));
    }
}
