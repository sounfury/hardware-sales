package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.InventoryLog;

/**
 * 库存流水服务接口
 */
public interface InventoryLogService extends IService<InventoryLog> {

    /** 分页查询库存流水，支持按商品、变动类型、日期范围筛选 */
    IPage<InventoryLog> pageQuery(Integer pageNum, Integer pageSize,
                                  Long productId, Integer type,
                                  String startDate, String endDate);
}
