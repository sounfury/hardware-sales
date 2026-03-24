package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.Supplier;
import com.hardware.sales.entity.SupplierProduct;
import com.hardware.sales.service.SupplierProductService;
import com.hardware.sales.service.SupplierService;
import com.hardware.sales.service.dto.SupplierProductUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 供应商商品控制器，管理供应商可供货的商品及报价
 */
@RestController
@RequestMapping("/api/supplier-product")
@RequiredArgsConstructor
public class SupplierProductController {

    private final SupplierProductService supplierProductService;
    private final SupplierService supplierService;

    /** 分页查询供应商商品，支持按供应商 ID、商品 ID、商品名称筛选。 */
    @GetMapping("/page")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<IPage<SupplierProduct>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName) {
        return Result.ok(supplierProductService.pageQuery(
                pageNum,
                pageSize,
                resolveSupplierScope(supplierId),
                productId,
                productName
        ));
    }

    /** 根据 ID 查询供应商商品详情 */
    @GetMapping("/{id}")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<SupplierProduct> getById(@PathVariable Long id) {
        SupplierProduct supplierProduct = requireSupplierProduct(id);
        if (StpUtil.hasRole("SUPPLIER")) {
            validateSupplierScope(supplierProduct.getSupplierId());
        }
        return Result.ok(supplierProduct);
    }

    /** 查询指定供应商的全部供货商品列表 */
    @GetMapping("/list")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<List<SupplierProduct>> listBySupplierId(@RequestParam Long supplierId) {
        return Result.ok(supplierProductService.listBySupplierId(resolveSupplierScope(supplierId)));
    }

    /** 查询指定商品的可供货供应商列表。 */
    @GetMapping("/by-product")
    @SaCheckRole("ADMIN")
    public Result<List<SupplierProduct>> listByProductId(@RequestParam Long productId) {
        return Result.ok(supplierProductService.listByProductId(productId));
    }

    /** 新增供应商商品 */
    @PostMapping
    @SaCheckRole("SUPPLIER")
    public Result<?> save(@RequestBody SupplierProductUpsertRequest request) {
        supplierProductService.createSupplierProduct(toSupplierProduct(request));
        return Result.ok();
    }

    /** 修改供应商商品 */
    @PutMapping
    @SaCheckRole("SUPPLIER")
    public Result<?> update(@RequestBody SupplierProductUpsertRequest request) {
        SupplierProduct existing = requireSupplierProduct(request.getId());
        validateSupplierScope(existing.getSupplierId());
        SupplierProduct supplierProduct = toSupplierProduct(request);
        supplierProduct.setSupplierId(existing.getSupplierId());
        supplierProductService.updateSupplierProduct(supplierProduct);
        return Result.ok();
    }

    /** 删除供应商商品 */
    @DeleteMapping("/{id}")
    @SaCheckRole("SUPPLIER")
    public Result<?> delete(@PathVariable Long id) {
        SupplierProduct existing = requireSupplierProduct(id);
        validateSupplierScope(existing.getSupplierId());
        supplierProductService.removeById(id);
        return Result.ok();
    }

    private Long resolveSupplierScope(Long supplierId) {
        if (!StpUtil.hasRole("SUPPLIER")) {
            return supplierId;
        }
        Supplier currentSupplier = requireCurrentSupplier();
        if (supplierId != null && !Objects.equals(supplierId, currentSupplier.getId())) {
            throw new BizException("仅支持查看当前供应商自己的报价");
        }
        return currentSupplier.getId();
    }

    private void validateSupplierScope(Long supplierId) {
        Supplier currentSupplier = requireCurrentSupplier();
        if (!Objects.equals(supplierId, currentSupplier.getId())) {
            throw new BizException("仅支持操作当前供应商自己的报价");
        }
    }

    private SupplierProduct requireSupplierProduct(Long id) {
        if (id == null) {
            throw new BizException("供应商商品ID不能为空");
        }
        SupplierProduct supplierProduct = supplierProductService.getById(id);
        if (supplierProduct == null) {
            throw new BizException("供应商商品不存在");
        }
        return supplierProduct;
    }

    private Supplier requireCurrentSupplier() {
        Supplier supplier = supplierService.lambdaQuery()
                .eq(Supplier::getUserId, StpUtil.getLoginIdAsLong())
                .one();
        if (supplier == null) {
            throw new BizException("当前供应商档案不存在");
        }
        if (supplier.getAuditStatus() == null || supplier.getAuditStatus() != 1) {
            throw new BizException("当前供应商尚未审核通过");
        }
        return supplier;
    }

    private SupplierProduct toSupplierProduct(SupplierProductUpsertRequest request) {
        SupplierProduct supplierProduct = new SupplierProduct();
        supplierProduct.setId(request.getId());
        supplierProduct.setSupplierId(requireCurrentSupplier().getId());
        supplierProduct.setProductId(request.getProductId());
        supplierProduct.setSupplyPrice(request.getSupplyPrice());
        supplierProduct.setRemark(request.getRemark());
        return supplierProduct;
    }
}
