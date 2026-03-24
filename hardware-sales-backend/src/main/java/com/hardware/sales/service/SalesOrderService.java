package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.SalesOrder;

/**
 * 销售出货服务接口
 */
public interface SalesOrderService extends IService<SalesOrder> {

    /** 分页查询销售单，支持按单号、客户名称、日期范围筛选 */
    IPage<SalesOrder> pageQuery(Integer pageNum, Integer pageSize,
                                String orderNo, String customerName,
                                String startDate, String endDate);

    /** 查询销售单详情（含销售明细列表） */
    SalesOrder detail(Long id);

    /** 创建销售单，同时保存明细、扣减库存、生成财务记录 */
    void createOrder(SalesOrder order);

    /** 结算销售单，同步更新关联财务记录状态 */
    void settle(Long id);
}
