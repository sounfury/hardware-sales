package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.Supplier;

/**
 * 供应商管理服务接口
 */
public interface SupplierService extends IService<Supplier> {

    /** 分页查询供应商，支持按企业名称、审核状态筛选 */
    IPage<Supplier> pageQuery(Integer pageNum, Integer pageSize,
                              String companyName, Integer auditStatus);

    /** 审核供应商，设置审核状态和备注 */
    void audit(Long id, Integer auditStatus, String auditRemark);
}
