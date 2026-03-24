package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.SupplierProduct;
import com.hardware.sales.service.SupplierProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商商品控制器，管理供应商可供货的商品及报价
 */
@RestController
@RequestMapping("/api/supplier-product")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class SupplierProductController {

    private final SupplierProductService supplierProductService;

    /** 分页查询供应商商品，支持按供应商 ID、商品 ID、商品名称筛选。 */
    @GetMapping("/page")
    public Result<IPage<SupplierProduct>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName) {
        return Result.ok(supplierProductService.pageQuery(pageNum, pageSize, supplierId, productId, productName));
    }

    /** 根据 ID 查询供应商商品详情 */
    @GetMapping("/{id}")
    public Result<SupplierProduct> getById(@PathVariable Long id) {
        return Result.ok(supplierProductService.getById(id));
    }

    /** 查询指定供应商的全部供货商品列表 */
    @GetMapping("/list")
    public Result<List<SupplierProduct>> listBySupplierId(@RequestParam Long supplierId) {
        return Result.ok(supplierProductService.listBySupplierId(supplierId));
    }

    /** 查询指定商品的可供货供应商列表。 */
    @GetMapping("/by-product")
    public Result<List<SupplierProduct>> listByProductId(@RequestParam Long productId) {
        return Result.ok(supplierProductService.listByProductId(productId));
    }

    /** 新增供应商商品 */
    @PostMapping
    public Result<?> save(@RequestBody SupplierProduct supplierProduct) {
        supplierProductService.createSupplierProduct(supplierProduct);
        return Result.ok();
    }

    /** 修改供应商商品 */
    @PutMapping
    public Result<?> update(@RequestBody SupplierProduct supplierProduct) {
        supplierProductService.updateSupplierProduct(supplierProduct);
        return Result.ok();
    }

    /** 删除供应商商品 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        supplierProductService.removeById(id);
        return Result.ok();
    }
}
