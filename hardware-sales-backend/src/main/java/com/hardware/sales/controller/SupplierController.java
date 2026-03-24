package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.Supplier;
import com.hardware.sales.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 供应商管理控制器，提供供应商的增删改查及审核操作
 */
@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /** 分页查询供应商，支持按企业名称、审核状态筛选 */
    @GetMapping("/page")
    @SaCheckRole("ADMIN")
    public Result<IPage<Supplier>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) Integer auditStatus) {
        return Result.ok(supplierService.pageQuery(pageNum, pageSize, companyName, auditStatus));
    }

    /** 根据 ID 查询供应商详情 */
    @GetMapping("/{id}")
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<Supplier> getById(@PathVariable Long id) {
        Supplier supplier = requireSupplier(id);
        if (StpUtil.hasRole("SUPPLIER")) {
            validateSupplierOwner(supplier.getId());
        }
        return Result.ok(supplier);
    }

    /** 新增供应商 */
    @PostMapping
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<?> save(@RequestBody Supplier supplier) {
        if (StpUtil.hasRole("SUPPLIER")) {
            if (findCurrentSupplier() != null) {
                throw new BizException("当前供应商档案已存在，请直接修改");
            }
            supplier.setUserId(StpUtil.getLoginIdAsLong());
            supplier.setAuditStatus(0);
            supplier.setAuditRemark(null);
        }
        supplierService.save(supplier);
        return Result.ok();
    }

    /** 修改供应商信息 */
    @PutMapping
    @SaCheckRole(value = {"ADMIN", "SUPPLIER"}, mode = SaMode.OR)
    public Result<?> update(@RequestBody Supplier supplier) {
        if (StpUtil.hasRole("SUPPLIER")) {
            Supplier existing = requireCurrentSupplier();
            supplier.setId(existing.getId());
            supplier.setUserId(existing.getUserId());
            supplier.setAuditStatus(existing.getAuditStatus());
            supplier.setAuditRemark(existing.getAuditRemark());
        }
        supplierService.updateById(supplier);
        return Result.ok();
    }

    /** 删除供应商 */
    @DeleteMapping("/{id}")
    @SaCheckRole("ADMIN")
    public Result<?> delete(@PathVariable Long id) {
        supplierService.removeById(id);
        return Result.ok();
    }

    /** 审核供应商：通过（1）或驳回（2），可附审核备注 */
    @PutMapping("/audit/{id}")
    @SaCheckRole("ADMIN")
    public Result<?> audit(@PathVariable Long id,
                           @RequestParam Integer auditStatus,
                           @RequestParam(required = false) String auditRemark) {
        supplierService.audit(id, auditStatus, auditRemark);
        return Result.ok();
    }

    private Supplier requireSupplier(Long id) {
        Supplier supplier = supplierService.getById(id);
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        return supplier;
    }

    private Supplier findCurrentSupplier() {
        return supplierService.lambdaQuery()
                .eq(Supplier::getUserId, StpUtil.getLoginIdAsLong())
                .one();
    }

    private Supplier requireCurrentSupplier() {
        Supplier supplier = findCurrentSupplier();
        if (supplier == null) {
            throw new BizException("当前供应商档案不存在");
        }
        return supplier;
    }

    private void validateSupplierOwner(Long supplierId) {
        Supplier currentSupplier = requireCurrentSupplier();
        if (!Objects.equals(supplierId, currentSupplier.getId())) {
            throw new BizException("仅支持查看当前供应商自己的档案");
        }
    }
}
