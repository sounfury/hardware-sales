package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.Product;
import com.hardware.sales.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理控制器，提供商品的分页查询、增删改
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class ProductController {

    private final ProductService productService;

    /** 分页查询商品，支持按名称、品牌、分类筛选 */
    @GetMapping("/page")
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
    public Result<Product> getById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    /** 新增商品 */
    @PostMapping
    public Result<?> save(@RequestBody Product product) {
        productService.saveProduct(product);
        return Result.ok();
    }

    /** 修改商品 */
    @PutMapping
    public Result<?> update(@RequestBody Product product) {
        productService.updateProduct(product);
        return Result.ok();
    }

    /** 管理员确认收货后，将商品从补货中恢复为正常状态 */
    @PutMapping("/restock-complete/{id}")
    public Result<?> completeRestock(@PathVariable Long id) {
        productService.completeRestock(id);
        return Result.ok();
    }

    /** 删除商品 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.ok();
    }
}
