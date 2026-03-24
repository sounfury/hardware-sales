package com.hardware.sales.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.ProductCategory;

import java.util.List;

/**
 * 商品分类服务接口
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /** 查询分类列表，可按状态过滤 */
    List<ProductCategory> listByStatus(Integer status);

    /** 新增分类并补齐默认状态 */
    void saveCategory(ProductCategory category);

    /** 修改分类基础信息 */
    void updateCategory(ProductCategory category);

    /** 启用或停用分类 */
    void updateStatus(Long id, Integer status);

    /** 将某分类下的商品批量迁移到目标分类 */
    long migrateProducts(Long sourceCategoryId, Long targetCategoryId);

    /** 删除分类，仅允许删除停用且空的分类 */
    void deleteCategory(Long id);
}
