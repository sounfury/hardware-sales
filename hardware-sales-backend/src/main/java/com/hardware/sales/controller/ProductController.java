package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.Product;
import com.hardware.sales.service.ProductService;
import com.hardware.sales.service.dto.ProductUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理控制器，提供商品的分页查询、增删改
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 分页查询商品，支持按名称、品牌、分类筛选 */
    @GetMapping("/page")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<IPage<Product>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long categoryId) {
        return Result.ok(productService.pageQuery(pageNum, pageSize, name, brand, categoryId));
    }

    /** 根据 ID 查询商品详情 */
    @GetMapping("/{id}")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<Product> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    /** 新增商品 */
    @PostMapping
    @SaCheckRole("ADMIN")
    public Result<?> save(@RequestBody ProductUpsertRequest request) {
        productService.saveProduct(toProduct(request));
        return Result.ok();
    }

    /** 修改商品 */
    @PutMapping
    @SaCheckRole("ADMIN")
    public Result<?> update(@RequestBody ProductUpsertRequest request) {
        productService.updateProduct(toProduct(request));
        return Result.ok();
    }

    /** 管理员确认收货后，将商品从补货中恢复为正常状态 */
    @PutMapping("/restock-complete/{id}")
    @SaCheckRole("ADMIN")
    public Result<?> completeRestock(@PathVariable Long id) {
        productService.completeRestock(id);
        return Result.ok();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    @SaCheckRole("ADMIN")
    public Result<?> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.ok();
    }

    private Product toProduct(ProductUpsertRequest request) {
        Product product = new Product();
        product.setId(request.getId());
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setBrand(request.getBrand());
        product.setSpec(request.getSpec());
        product.setDescription(request.getDescription());
        product.setUnit(request.getUnit());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSalePrice(request.getSalePrice());
        product.setRestockThreshold(request.getRestockThreshold());
        return product;
    }
}
