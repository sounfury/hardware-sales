package com.hardware.sales.service.impl;

import com.hardware.sales.common.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.entity.Product;
import com.hardware.sales.entity.ProductCategory;
import com.hardware.sales.service.ProductCategoryService;
import com.hardware.sales.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 商品分类服务测试，验证停用、迁移、删除与商品绑定规则。
 */
@SpringBootTest
@Transactional
class ProductCategoryServiceImplTest {

    @Autowired
    private ProductCategoryService productCategoryService;

    @Autowired
    private ProductService productService;

    /**
     * 启用中的分类即使没有商品，也必须先停用后才能删除。
     */
    @Test
    void shouldRejectDeletingEnabledCategoryEvenWhenEmpty() {
        ProductCategory category = createCategory("待删启用分类", 1);

        BizException exception = assertThrows(BizException.class,
                () -> productCategoryService.deleteCategory(category.getId()));

        assertEquals("仅允许删除已停用的分类", exception.getMessage());
    }

    /**
     * 停用分类后，如果仍然挂有商品，也必须先迁移再删除。
     */
    @Test
    void shouldRejectDeletingDisabledCategoryWithProducts() {
        productCategoryService.updateStatus(1L, 0);

        BizException exception = assertThrows(BizException.class,
                () -> productCategoryService.deleteCategory(1L));

        assertEquals("分类下仍有商品，不能删除，请先迁移商品", exception.getMessage());
    }

    /**
     * 分类下商品迁移完成后，停用中的空分类应允许删除。
     */
    @Test
    void shouldDeleteDisabledEmptyCategoryAfterMigration() {
        ProductCategory sourceCategory = createCategory("待迁移源分类", 1);
        ProductCategory targetCategory = createCategory("迁移目标分类", 1);
        Product product = createProduct(sourceCategory.getId(), "迁移测试商品");

        productCategoryService.updateStatus(sourceCategory.getId(), 0);
        long movedCount = productCategoryService.migrateProducts(sourceCategory.getId(), targetCategory.getId());
        productCategoryService.deleteCategory(sourceCategory.getId());

        Product savedProduct = productService.getById(product.getId());
        assertEquals(1L, movedCount);
        assertNotNull(savedProduct);
        assertEquals(targetCategory.getId(), savedProduct.getCategoryId());
        assertNull(productCategoryService.getById(sourceCategory.getId()));
    }

    /**
     * 新增商品时不允许直接绑定停用中的分类。
     */
    @Test
    void shouldRejectCreatingProductUnderDisabledCategory() {
        ProductCategory category = createCategory("停用分类", 1);
        productCategoryService.updateStatus(category.getId(), 0);

        BizException exception = assertThrows(BizException.class,
                () -> productService.saveProduct(buildProduct(category.getId(), "停用分类商品")));

        assertEquals("商品分类已停用，请重新选择", exception.getMessage());
    }

    /**
     * 已经挂在停用分类上的老商品，若不更换分类，仍允许编辑其他字段。
     */
    @Test
    void shouldAllowUpdatingProductWithoutChangingDisabledCurrentCategory() {
        ProductCategory category = createCategory("编辑停用分类", 1);
        Product product = createProduct(category.getId(), "编辑前商品");
        productCategoryService.updateStatus(category.getId(), 0);

        Product updatingProduct = new Product();
        updatingProduct.setId(product.getId());
        updatingProduct.setCategoryId(category.getId());
        updatingProduct.setName("编辑后商品");
        updatingProduct.setBrand(product.getBrand());
        updatingProduct.setUnit(product.getUnit());
        updatingProduct.setPurchasePrice(product.getPurchasePrice());
        updatingProduct.setSalePrice(product.getSalePrice());

        assertDoesNotThrow(() -> productService.updateProduct(updatingProduct));
        assertEquals("编辑后商品", productService.getById(product.getId()).getName());
    }

    /**
     * 商品分页查询应忽略客户端误传的 undefined 关键字，避免首屏查询被错误过滤。
     */
    @Test
    void shouldIgnoreLiteralUndefinedKeywordWhenPagingProducts() {
        ProductCategory category = createCategory("分页兜底分类", 1);
        createProduct(category.getId(), "分页兜底商品");

        IPage<Product> normalPage = productService.pageQuery(1, 100, null, null, null);
        IPage<Product> undefinedKeywordPage = productService.pageQuery(1, 100, "undefined", null, null);

        assertEquals(normalPage.getTotal(), undefinedKeywordPage.getTotal());
    }

    private ProductCategory createCategory(String name, Integer status) {
        ProductCategory category = new ProductCategory();
        category.setName(name + System.nanoTime());
        category.setSort(99);
        category.setStatus(status);
        productCategoryService.saveCategory(category);
        return category;
    }

    private Product createProduct(Long categoryId, String name) {
        Product product = buildProduct(categoryId, name);
        productService.saveProduct(product);
        return product;
    }

    private Product buildProduct(Long categoryId, String name) {
        Product product = new Product();
        product.setCategoryId(categoryId);
        product.setName(name + System.nanoTime());
        product.setBrand("测试品牌");
        product.setUnit("件");
        product.setPurchasePrice(new BigDecimal("10.00"));
        product.setSalePrice(new BigDecimal("15.00"));
        product.setStock(5);
        product.setRestockThreshold(2);
        return product;
    }
}
