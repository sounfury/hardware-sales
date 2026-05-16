package com.hardware.sales.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Product;
import com.hardware.sales.entity.ProductCategory;
import com.hardware.sales.mapper.ProductCategoryMapper;
import com.hardware.sales.mapper.ProductMapper;
import com.hardware.sales.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.Objects;

/**
 * 商品管理服务实现
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {

    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public IPage<Product> pageQuery(Integer pageNum, Integer pageSize,
                                    String name, String brand, Long categoryId) {
        String normalizedName = normalizeQueryKeyword(name);
        String normalizedBrand = normalizeQueryKeyword(brand);
        return lambdaQuery()
                .like(StrUtil.isNotBlank(normalizedName), Product::getName, normalizedName)
                .like(StrUtil.isNotBlank(normalizedBrand), Product::getBrand, normalizedBrand)
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .orderByDesc(Product::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    /** 新增商品并补齐默认补货字段。 */
    @Override
    @Transactional
    public void saveProduct(Product product) {
        validateCategoryForCreate(product.getCategoryId());
        normalizeForCreate(product);
        save(product);
    }

    /** 修改商品并保留已有补货状态信息。 */
    @Override
    @Transactional
    public void updateProduct(Product product) {
        if (product.getId() == null) {
            throw new BizException("商品ID不能为空");
        }
        Product existing = requireProduct(product.getId());
        validateCategoryForUpdate(product.getCategoryId(), existing.getCategoryId());
        normalizeForUpdate(product, existing);
        updateById(product);
    }

    /** 将商品标记为补货中，并记录当前跟进供应商。 */
    @Override
    @Transactional
    public void markRestocking(Long productId, Long supplierId) {
        Product product = requireProduct(productId);
        if (product.getRestockStatus() != null && product.getRestockStatus() == 1) {
            throw new BizException("当前商品已处于补货中，请先完成当前补货");
        }
        lambdaUpdate()
                .eq(Product::getId, productId)
                .set(Product::getRestockStatus, 1)
                .set(Product::getRestockSupplierId, supplierId)
                .update();
    }

    /** 管理员确认收货后，将商品恢复为正常状态。 */
    @Override
    @Transactional
    public void completeRestock(Long productId) {
        Product product = requireProduct(productId);
        if (product.getRestockStatus() == null || product.getRestockStatus() != 1) {
            throw new BizException("当前商品未处于补货中");
        }
        lambdaUpdate()
                .eq(Product::getId, productId)
                .set(Product::getRestockStatus, 0)
                .set(Product::getRestockSupplierId, null)
                .update();
    }

    private void normalizeForCreate(Product product) {
        validateThreshold(product.getRestockThreshold());
        if (product.getRestockThreshold() == null) {
            product.setRestockThreshold(10);
        }
        if (product.getRestockStatus() == null) {
            product.setRestockStatus(0);
        }
        if (product.getRestockStatus() == 0) {
            product.setRestockSupplierId(null);
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
    }

    private void normalizeForUpdate(Product product, Product existing) {
        Integer threshold = product.getRestockThreshold() != null
                ? product.getRestockThreshold()
                : existing.getRestockThreshold();
        validateThreshold(threshold);
        product.setRestockThreshold(threshold != null ? threshold : 10);

        Integer restockStatus = product.getRestockStatus() != null
                ? product.getRestockStatus()
                : existing.getRestockStatus();
        product.setRestockStatus(restockStatus != null ? restockStatus : 0);

        Long restockSupplierId = product.getRestockSupplierId() != null
                ? product.getRestockSupplierId()
                : existing.getRestockSupplierId();
        product.setRestockSupplierId(product.getRestockStatus() == 1 ? restockSupplierId : null);
    }

    private void validateThreshold(Integer restockThreshold) {
        if (restockThreshold != null && restockThreshold < 0) {
            throw new BizException("补货阈值不能小于0");
        }
    }

    /** 统一兜底小程序等客户端误传的空关键字，避免拼出 like '%undefined%'。 */
    private String normalizeQueryKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return null;
        }
        String trimmedKeyword = keyword.trim();
        if ("undefined".equalsIgnoreCase(trimmedKeyword) || "null".equalsIgnoreCase(trimmedKeyword)) {
            return null;
        }
        return trimmedKeyword;
    }

    private Product requireProduct(Long productId) {
        Product product = getById(productId);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        return product;
    }

    private void validateCategoryForCreate(Long categoryId) {
        ProductCategory category = requireCategory(categoryId);
        if (category.getStatus() == null || category.getStatus() != 1) {
            throw new BizException("商品分类已停用，请重新选择");
        }
    }

    private void validateCategoryForUpdate(Long targetCategoryId, Long existingCategoryId) {
        Long finalCategoryId = targetCategoryId != null ? targetCategoryId : existingCategoryId;
        ProductCategory category = requireCategory(finalCategoryId);
        if (!Objects.equals(finalCategoryId, existingCategoryId)
                && (category.getStatus() == null || category.getStatus() != 1)) {
            throw new BizException("商品分类已停用，请重新选择");
        }
    }

    private ProductCategory requireCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BizException("商品分类不能为空");
        }
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException("商品分类不存在");
        }
        return category;
    }
}
