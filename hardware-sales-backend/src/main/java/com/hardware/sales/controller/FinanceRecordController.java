package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.FinanceRecord;
import com.hardware.sales.service.FinanceRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 财务收支控制器，提供收支记录查询、应收应付汇总
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class FinanceRecordController {

    private final FinanceRecordService financeRecordService;

    /** 分页查询收支记录，支持按类型（收入/支出）、结算状态、日期范围筛选 */
    @GetMapping("/page")
    public Result<IPage<FinanceRecord>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(financeRecordService.pageQuery(pageNum, pageSize, type, paymentStatus, startDate, endDate));
    }

    /** 查询客户应收款总额（未结算的销售收入） */
    @GetMapping("/receivable")
    public Result<BigDecimal> receivable() {
        return Result.ok(financeRecordService.receivableTotal());
    }

    /** 查询供应商应付款总额（未结算的采购支出） */
    @GetMapping("/payable")
    public Result<BigDecimal> payable() {
        return Result.ok(financeRecordService.payableTotal());
    }

    /** 查询收支汇总（总收入、总支出、净利润） */
    @GetMapping("/summary")
    public Result<Map<String, BigDecimal>> summary() {
        return Result.ok(financeRecordService.summary());
    }
}
