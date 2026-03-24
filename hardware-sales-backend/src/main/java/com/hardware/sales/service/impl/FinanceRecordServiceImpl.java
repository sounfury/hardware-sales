package com.hardware.sales.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hardware.sales.entity.FinanceRecord;
import com.hardware.sales.mapper.FinanceRecordMapper;
import com.hardware.sales.service.FinanceRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务收支服务实现，提供收支查询和汇总统计
 */
@Service
public class FinanceRecordServiceImpl extends ServiceImpl<FinanceRecordMapper, FinanceRecord>
        implements FinanceRecordService {

    @Override
    public IPage<FinanceRecord> pageQuery(Integer pageNum, Integer pageSize,
                                          Integer type, Integer paymentStatus,
                                          String startDate, String endDate) {
        return lambdaQuery()
                .eq(type != null, FinanceRecord::getType, type)
                .eq(paymentStatus != null, FinanceRecord::getPaymentStatus, paymentStatus)
                .ge(StrUtil.isNotBlank(startDate), FinanceRecord::getCreateTime, startDate)
                .le(StrUtil.isNotBlank(endDate), FinanceRecord::getCreateTime, endDate)
                .orderByDesc(FinanceRecord::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    @Override
    public BigDecimal receivableTotal() {
        // 未结算的销售收入
        return lambdaQuery()
                .eq(FinanceRecord::getType, 1)
                .eq(FinanceRecord::getPaymentStatus, 0)
                .list()
                .stream()
                .map(FinanceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal payableTotal() {
        // 未结算的采购支出
        return lambdaQuery()
                .eq(FinanceRecord::getType, 2)
                .eq(FinanceRecord::getPaymentStatus, 0)
                .list()
                .stream()
                .map(FinanceRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Map<String, BigDecimal> summary() {
        Map<String, BigDecimal> result = new HashMap<>();
        // 总收入
        BigDecimal totalIncome = lambdaQuery().eq(FinanceRecord::getType, 1).list()
                .stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // 总支出
        BigDecimal totalExpense = lambdaQuery().eq(FinanceRecord::getType, 2).list()
                .stream().map(FinanceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalIncome", totalIncome);
        result.put("totalExpense", totalExpense);
        result.put("profit", totalIncome.subtract(totalExpense));
        result.put("receivable", receivableTotal());
        result.put("payable", payableTotal());
        return result;
    }
}
