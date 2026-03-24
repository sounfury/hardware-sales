package com.hardware.sales.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Supplier;
import com.hardware.sales.mapper.SupplierMapper;
import com.hardware.sales.service.SupplierService;
import org.springframework.stereotype.Service;

/**
 * 供应商管理服务实现
 */
@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier>
        implements SupplierService {

    @Override
    public IPage<Supplier> pageQuery(Integer pageNum, Integer pageSize,
                                     String companyName, Integer auditStatus) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(companyName), Supplier::getCompanyName, companyName)
                .eq(auditStatus != null, Supplier::getAuditStatus, auditStatus)
                .orderByDesc(Supplier::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    @Override
    public void audit(Long id, Integer auditStatus, String auditRemark) {
        Supplier supplier = getById(id);
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        if (auditStatus != 1 && auditStatus != 2) {
            throw new BizException("审核状态不合法");
        }
        supplier.setAuditStatus(auditStatus);
        supplier.setAuditRemark(auditRemark);
        updateById(supplier);
    }
}
