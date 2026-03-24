package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.Supplier;
import com.hardware.sales.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商管理控制器，提供供应商的增删改查及审核操作
 */
@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class SupplierController {

    private final SupplierService supplierService;

    /** 分页查询供应商，支持按企业名称、审核状态筛选 */
    @GetMapping("/page")
    public Result<IPage<Supplier>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) Integer auditStatus) {
        return Result.ok(supplierService.pageQuery(pageNum, pageSize, companyName, auditStatus));
    }

    /** 根据 ID 查询供应商详情 */
    @GetMapping("/{id}")
    public Result<Supplier> getById(@PathVariable Long id) {
        return Result.ok(supplierService.getById(id));
    }

    /** 新增供应商 */
    @PostMapping
    public Result<?> save(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return Result.ok();
    }

    /** 修改供应商信息 */
    @PutMapping
    public Result<?> update(@RequestBody Supplier supplier) {
        supplierService.updateById(supplier);
        return Result.ok();
    }

    /** 删除供应商 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        supplierService.removeById(id);
        return Result.ok();
    }

    /** 审核供应商：通过（1）或驳回（2），可附审核备注 */
    @PutMapping("/audit/{id}")
    public Result<?> audit(@PathVariable Long id,
                           @RequestParam Integer auditStatus,
                           @RequestParam(required = false) String auditRemark) {
        supplierService.audit(id, auditStatus, auditRemark);
        return Result.ok();
    }
}
