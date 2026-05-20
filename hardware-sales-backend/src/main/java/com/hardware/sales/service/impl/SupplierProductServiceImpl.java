package com.hardware.sales.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.Product;
import com.hardware.sales.entity.Supplier;
import com.hardware.sales.entity.SupplierProduct;
import com.hardware.sales.mapper.SupplierProductMapper;
import com.hardware.sales.service.ProductService;
import com.hardware.sales.service.SupplierProductService;
import com.hardware.sales.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商商品服务实现，负责维护供应商与系统商品之间的真实关联关系。
 */
@Service
@RequiredArgsConstructor
public class SupplierProductServiceImpl extends ServiceImpl<SupplierProductMapper, SupplierProduct>
        implements SupplierProductService {

    private final SupplierService supplierService;
    private final ProductService productService;

    /** 分页查询供应商商品，并补充供应商名和商品展示信息。 */
    @Override
    public IPage<SupplierProduct> pageQuery(Integer pageNum, Integer pageSize,
                                            Long supplierId, Long productId, String productName) {
        MPJLambdaWrapper<SupplierProduct> wrapper = buildJoinWrapper()
                .eq(supplierId != null, SupplierProduct::getSupplierId, supplierId)
                .eq(productId != null, SupplierProduct::getProductId, productId)
                .like(StrUtil.isNotBlank(productName), Product::getName, productName)
                .orderByDesc(SupplierProduct::getCreateTime);
        return baseMapper.selectJoinPage(new Page<>(pageNum, pageSize), SupplierProduct.class, wrapper);
    }

    /** 按 ID 联表查询报价详情，补充商品名称、规格、单位等展示字段。 */
    @Override
    public SupplierProduct getDetailById(Long id) {
        return baseMapper.selectJoinOne(SupplierProduct.class, buildJoinWrapper()
                .eq(SupplierProduct::getId, id));
    }

    /** 查询指定供应商维护的全部供货商品。 */
    @Override
    public List<SupplierProduct> listBySupplierId(Long supplierId) {
        return baseMapper.selectJoinList(SupplierProduct.class, buildJoinWrapper()
                .eq(SupplierProduct::getSupplierId, supplierId)
                .orderByDesc(SupplierProduct::getCreateTime));
    }

    /** 查询某个商品的可供货供应商列表，仅返回审核通过的供应商。 */
    @Override
    public List<SupplierProduct> listByProductId(Long productId) {
        return baseMapper.selectJoinList(SupplierProduct.class, buildJoinWrapper()
                .eq(SupplierProduct::getProductId, productId)
                .eq(Supplier::getAuditStatus, 1)
                .orderByAsc(SupplierProduct::getSupplyPrice)
                .orderByDesc(SupplierProduct::getCreateTime));
    }

    /** 新增供应商商品关联。 */
    @Override
    public void createSupplierProduct(SupplierProduct supplierProduct) {
        validateSupplierProduct(supplierProduct, false);
        save(supplierProduct);
    }

    /** 修改供应商商品关联。 */
    @Override
    public void updateSupplierProduct(SupplierProduct supplierProduct) {
        if (supplierProduct.getId() == null) {
            throw new BizException("供应商商品ID不能为空");
        }
        if (getById(supplierProduct.getId()) == null) {
            throw new BizException("供应商商品不存在");
        }
        validateSupplierProduct(supplierProduct, true);
        updateById(supplierProduct);
    }

    /**
     * 构建统一联表查询，避免各接口分别拼装供应商与商品展示字段。
     */
    private MPJLambdaWrapper<SupplierProduct> buildJoinWrapper() {
        return new MPJLambdaWrapper<SupplierProduct>()
                .selectAll(SupplierProduct.class)
                .selectAs(Supplier::getUserId, SupplierProduct::getSupplierUserId)
                .selectAs(Supplier::getCompanyName, SupplierProduct::getSupplierName)
                .selectAs(Product::getName, SupplierProduct::getProductName)
                .selectAs(Product::getSpec, SupplierProduct::getProductSpec)
                .selectAs(Product::getUnit, SupplierProduct::getProductUnit)
                .leftJoin(Supplier.class, Supplier::getId, SupplierProduct::getSupplierId)
                .leftJoin(Product.class, Product::getId, SupplierProduct::getProductId);
    }

    /**
     * 校验供应商商品关系是否完整、引用是否存在，以及唯一键是否冲突。
     */
    private void validateSupplierProduct(SupplierProduct supplierProduct, boolean isUpdate) {
        if (supplierProduct.getSupplierId() == null) {
            throw new BizException("供应商不能为空");
        }
        if (supplierProduct.getProductId() == null) {
            throw new BizException("商品不能为空");
        }
        if (supplierProduct.getSupplyPrice() == null) {
            throw new BizException("供货价格不能为空");
        }
        if (supplierService.getById(supplierProduct.getSupplierId()) == null) {
            throw new BizException("供应商不存在");
        }
        if (productService.getById(supplierProduct.getProductId()) == null) {
            throw new BizException("商品不存在");
        }

        // 干净重构版直接依赖数据库唯一键语义，服务层提前报出更明确的业务错误。
        long duplicateCount = lambdaQuery()
                .eq(SupplierProduct::getSupplierId, supplierProduct.getSupplierId())
                .eq(SupplierProduct::getProductId, supplierProduct.getProductId())
                .ne(isUpdate && supplierProduct.getId() != null, SupplierProduct::getId, supplierProduct.getId())
                .count();
        if (duplicateCount > 0) {
            throw new BizException("该供应商的商品报价已存在，请直接编辑");
        }
    }
}
