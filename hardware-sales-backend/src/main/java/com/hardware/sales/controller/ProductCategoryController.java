package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.ProductCategory;
import com.hardware.sales.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类控制器，提供分类的增删改查
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    /** 查询全部分类（按排序字段升序） */
    @GetMapping("/list")
    public Result<List<ProductCategory>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(categoryService.listByStatus(status));
    }

    /** 新增分类 */
    @PostMapping
    public Result<?> save(@RequestBody ProductCategory category) {
        categoryService.saveCategory(category);
        return Result.ok();
    }

    /** 修改分类 */
    @PutMapping
    public Result<?> update(@RequestBody ProductCategory category) {
        categoryService.updateCategory(category);
        return Result.ok();
    }

    /** 启用或停用分类 */
    @PutMapping("/{id}/status/{status}")
    public Result<?> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        categoryService.updateStatus(id, status);
        return Result.ok();
    }

    /** 批量迁移分类下的商品 */
    @PutMapping("/{id}/migrate/{targetCategoryId}")
    public Result<Long> migrateProducts(@PathVariable Long id, @PathVariable Long targetCategoryId) {
        return Result.ok(categoryService.migrateProducts(id, targetCategoryId));
    }

    /** 删除分类，仅允许删除停用且空的分类 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok();
    }
}
