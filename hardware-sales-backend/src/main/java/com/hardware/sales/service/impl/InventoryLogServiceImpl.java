package com.hardware.sales.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.hardware.sales.entity.InventoryLog;
import com.hardware.sales.entity.Product;
import com.hardware.sales.mapper.InventoryLogMapper;
import com.hardware.sales.service.InventoryLogService;
import org.springframework.stereotype.Service;

/**
 * 库存流水服务实现，联表查询商品名称
 */
@Service
public class InventoryLogServiceImpl extends ServiceImpl<InventoryLogMapper, InventoryLog>
        implements InventoryLogService {

    @Override
    public IPage<InventoryLog> pageQuery(Integer pageNum, Integer pageSize,
                                         Long productId, Integer type,
                                         String startDate, String endDate) {
        MPJLambdaWrapper<InventoryLog> wrapper = new MPJLambdaWrapper<InventoryLog>()
                .selectAll(InventoryLog.class)
                .selectAs(Product::getName, InventoryLog::getProductName)
                .leftJoin(Product.class, Product::getId, InventoryLog::getProductId)
                .eq(productId != null, InventoryLog::getProductId, productId)
                .eq(type != null, InventoryLog::getType, type)
                .ge(startDate != null, InventoryLog::getCreateTime, startDate)
                .le(endDate != null, InventoryLog::getCreateTime, endDate)
                .orderByDesc(InventoryLog::getCreateTime);
        return baseMapper.selectJoinPage(new Page<>(pageNum, pageSize), InventoryLog.class, wrapper);
    }
}
