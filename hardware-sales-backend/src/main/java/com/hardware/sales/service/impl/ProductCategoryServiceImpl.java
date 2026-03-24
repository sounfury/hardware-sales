package com.hardware.sales.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Product;
import com.hardware.sales.entity.ProductCategory;
import com.hardware.sales.mapper.ProductMapper;
import com.hardware.sales.mapper.ProductCategoryMapper;
import com.hardware.sales.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类服务实现，直接复用 MyBatis-Plus 通用 CRUD
 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    private final ProductMapper productMapper;

    @Override
    public List<ProductCategory> listByStatus(Integer status) {
        List<ProductCategory> categories = lambdaQuery()
                .eq(status != null, ProductCategory::getStatus, status)
                .orderByAsc(ProductCategory::getSort)
                .orderByAsc(ProductCategory::getId)
                .list();
        fillProductCount(categories);
        return categories;
    }

    @Override
    @Transactional
    public void saveCategory(ProductCategory category) {
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        validateStatus(category.getStatus());
        save(category);
    }

    @Override
    @Transactional
    public void updateCategory(ProductCategory category) {
        if (category.getId() == null) {
            throw new BizException("分类ID不能为空");
        }
        ProductCategory existing = requireCategory(category.getId());
        if (category.getSort() == null) {
            category.setSort(existing.getSort());
        }
        if (category.getStatus() == null) {
            category.setStatus(existing.getStatus());
        }
        validateStatus(category.getStatus());
        updateById(category);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        requireCategory(id);
        lambdaUpdate()
                .eq(ProductCategory::getId, id)
                .set(ProductCategory::getStatus, status)
                .update();
    }

    @Override
    @Transactional
    public long migrateProducts(Long sourceCategoryId, Long targetCategoryId) {
        if (sourceCategoryId == null || targetCategoryId == null) {
            throw new BizException("源分类和目标分类不能为空");
        }
        if (sourceCategoryId.equals(targetCategoryId)) {
            throw new BizException("目标分类不能与源分类相同");
        }
        requireCategory(sourceCategoryId);
        ProductCategory targetCategory = requireCategory(targetCategoryId);
        if (targetCategory.getStatus() == null || targetCategory.getStatus() != 1) {
            throw new BizException("目标分类已停用，请选择启用中的分类");
        }
        return productMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>()
                .eq(Product::getCategoryId, sourceCategoryId)
                .set(Product::getCategoryId, targetCategoryId));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        ProductCategory category = requireCategory(id);
        if (category.getStatus() == null || category.getStatus() != 0) {
            throw new BizException("仅允许删除已停用的分类");
        }
        Long productCount = productMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BizException("分类下仍有商品，不能删除，请先迁移商品");
        }
        if (!removeById(id)) {
            throw new BizException("分类删除失败，请稍后重试");
        }
    }

    private ProductCategory requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BizException("分类ID不能为空");
        }
        ProductCategory category = getById(categoryId);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        return category;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("分类状态非法");
        }
    }

    private void fillProductCount(List<ProductCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        List<Map<String, Object>> countRows = productMapper.selectMaps(new QueryWrapper<Product>()
                .select("category_id AS categoryId", "COUNT(*) AS total")
                .isNotNull("category_id")
                .groupBy("category_id"));
        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : countRows) {
            Object categoryId = row.get("categoryId");
            Object total = row.get("total");
            if (categoryId instanceof Number categoryNumber && total instanceof Number totalNumber) {
                countMap.put(categoryNumber.longValue(), totalNumber.longValue());
            }
        }
        for (ProductCategory category : categories) {
            category.setProductCount(countMap.getOrDefault(category.getId(), 0L));
        }
    }
}
