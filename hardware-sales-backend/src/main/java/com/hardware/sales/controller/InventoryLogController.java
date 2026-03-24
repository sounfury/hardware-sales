package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.InventoryLog;
import com.hardware.sales.service.InventoryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存流水控制器，提供库存变动记录的分页查询
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    /** 分页查询库存流水，支持按商品、类型（入库/出库）、日期范围筛选 */
    @GetMapping("/log/page")
    public Result<IPage<InventoryLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(inventoryLogService.pageQuery(pageNum, pageSize, productId, type, startDate, endDate));
    }
}
