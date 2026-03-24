package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.FinanceRecord;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 财务收支服务接口
 */
public interface FinanceRecordService extends IService<FinanceRecord> {

    /** 分页查询收支记录，支持按类型、结算状态、日期范围筛选 */
    IPage<FinanceRecord> pageQuery(Integer pageNum, Integer pageSize,
                                   Integer type, Integer paymentStatus,
                                   String startDate, String endDate);

    /** 应收款汇总（未结算的销售收入） */
    BigDecimal receivableTotal();

    /** 应付款汇总（未结算的采购支出） */
    BigDecimal payableTotal();

    /** 收支汇总统计 */
    Map<String, BigDecimal> summary();
}
