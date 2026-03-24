package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.Product;

/**
 * 商品管理服务接口
 */
public interface ProductService extends IService<Product> {

    /** 分页查询商品，支持按名称、品牌、分类筛选 */
    IPage<Product> pageQuery(Integer pageNum, Integer pageSize,
                             String name, String brand, Long categoryId);

    /** 新增商品并补齐默认补货字段 */
    void saveProduct(Product product);

    /** 修改商品并保留已有补货状态信息 */
    void updateProduct(Product product);

    /** 将商品标记为补货中，并记录当前处理供应商 */
    void markRestocking(Long productId, Long supplierId);

    /** 管理员确认收货后，将商品恢复为正常状态 */
    void completeRestock(Long productId);
}
