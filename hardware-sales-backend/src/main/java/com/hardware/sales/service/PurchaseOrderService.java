package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.PurchaseOrder;

/**
 * 采购进货服务接口
 */
public interface PurchaseOrderService extends IService<PurchaseOrder> {

    /** 分页查询采购单，支持按单号、供应商、日期范围筛选 */
    IPage<PurchaseOrder> pageQuery(Integer pageNum, Integer pageSize,
                                   String orderNo, Long supplierId,
                                   String startDate, String endDate);

    /** 查询采购单详情（含采购明细列表） */
    PurchaseOrder detail(Long id);

    /** 创建采购单，同时保存明细、更新库存、生成财务记录 */
    void createOrder(PurchaseOrder order);

    /** 结算采购单，同步更新关联财务记录状态 */
    void settle(Long id);
}
