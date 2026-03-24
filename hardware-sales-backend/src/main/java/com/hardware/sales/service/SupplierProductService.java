package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.SupplierProduct;

import java.util.List;

/**
 * 供应商商品服务接口
 */
public interface SupplierProductService extends IService<SupplierProduct> {

    /** 分页查询供应商商品，支持按供应商 ID、商品 ID、商品名称筛选 */
    IPage<SupplierProduct> pageQuery(Integer pageNum, Integer pageSize,
                                     Long supplierId, Long productId, String productName);

    /** 查询指定供应商的全部供货商品 */
    List<SupplierProduct> listBySupplierId(Long supplierId);

    /** 查询指定商品的全部可供货供应商 */
    List<SupplierProduct> listByProductId(Long productId);

    /** 新增供应商商品 */
    void createSupplierProduct(SupplierProduct supplierProduct);

    /** 修改供应商商品 */
    void updateSupplierProduct(SupplierProduct supplierProduct);
}
