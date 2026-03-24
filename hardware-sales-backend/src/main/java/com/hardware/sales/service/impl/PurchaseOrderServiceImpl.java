package com.hardware.sales.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.*;
import com.hardware.sales.mapper.PurchaseItemMapper;
import com.hardware.sales.mapper.PurchaseOrderMapper;
import com.hardware.sales.service.FinanceRecordService;
import com.hardware.sales.service.InventoryLogService;
import com.hardware.sales.service.ProductService;
import com.hardware.sales.service.PurchaseOrderService;
import com.hardware.sales.service.SupplierProductService;
import com.hardware.sales.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购进货服务实现，包含创建采购单（自动入库 + 生成财务记录）和结算
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder>
        implements PurchaseOrderService {

    private final PurchaseItemMapper purchaseItemMapper;
    private final ProductService productService;
    private final InventoryLogService inventoryLogService;
    private final FinanceRecordService financeRecordService;
    private final SupplierService supplierService;
    private final SupplierProductService supplierProductService;

    /** 分页查询采购单，补充供应商名称用于列表展示。 */
    @Override
    public IPage<PurchaseOrder> pageQuery(Integer pageNum, Integer pageSize,
                                          String orderNo, Long supplierId,
                                          String startDate, String endDate) {
        MPJLambdaWrapper<PurchaseOrder> wrapper = new MPJLambdaWrapper<PurchaseOrder>()
                .selectAll(PurchaseOrder.class)
                .selectAs(Supplier::getCompanyName, PurchaseOrder::getSupplierName)
                .leftJoin(Supplier.class, Supplier::getId, PurchaseOrder::getSupplierId)
                .like(StrUtil.isNotBlank(orderNo), PurchaseOrder::getOrderNo, orderNo)
                .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .ge(startDate != null, PurchaseOrder::getOrderDate, startDate)
                .le(endDate != null, PurchaseOrder::getOrderDate, endDate)
                .orderByDesc(PurchaseOrder::getCreateTime);
        return baseMapper.selectJoinPage(new Page<>(pageNum, pageSize), PurchaseOrder.class, wrapper);
    }

    /** 查询采购单详情，并返回采购明细与供应商名称。 */
    @Override
    public PurchaseOrder detail(Long id) {
        PurchaseOrder order = getById(id);
        if (order == null) {
            throw new BizException("采购单不存在");
        }
        // 查询明细，联表获取商品名称
        MPJLambdaWrapper<PurchaseItem> wrapper = new MPJLambdaWrapper<PurchaseItem>()
                .selectAll(PurchaseItem.class)
                .selectAs(Product::getName, PurchaseItem::getProductName)
                .leftJoin(Product.class, Product::getId, PurchaseItem::getProductId)
                .eq(PurchaseItem::getOrderId, id);
        List<PurchaseItem> items = purchaseItemMapper.selectJoinList(PurchaseItem.class, wrapper);
        order.setItems(items);

        // 补充供应商名称
        Supplier supplier = new Supplier();
        MPJLambdaWrapper<PurchaseOrder> orderWrapper = new MPJLambdaWrapper<PurchaseOrder>()
                .selectAs(Supplier::getCompanyName, PurchaseOrder::getSupplierName)
                .leftJoin(Supplier.class, Supplier::getId, PurchaseOrder::getSupplierId)
                .eq(PurchaseOrder::getId, id);
        PurchaseOrder joined = baseMapper.selectJoinOne(PurchaseOrder.class, orderWrapper);
        if (joined != null) {
            order.setSupplierName(joined.getSupplierName());
        }
        return order;
    }

    /** 创建采购单，强制复用供应商商品中维护的供货价并同步入库、财务数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(PurchaseOrder order) {
        validateCreateOrder(order);

        List<PurchaseItem> items = order.getItems();
        Map<Long, SupplierProduct> supplierProductMap = resolveSupplierProductMap(order.getSupplierId(), items);

        // 生成单号
        String orderNo = "PO" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
        order.setOrderNo(orderNo);

        // 采购单不允许前端自行改价，统一以供应商商品里维护的供货价为准。
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseItem item : items) {
            SupplierProduct supplierProduct = supplierProductMap.get(item.getProductId());
            item.setPrice(supplierProduct.getSupplyPrice());
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            totalAmount = totalAmount.add(item.getAmount());
        }
        order.setTotalAmount(totalAmount);
        order.setPaymentStatus(0);

        // 保存主单
        save(order);

        // 保存明细 + 更新库存 + 记录流水
        for (PurchaseItem item : items) {
            item.setOrderId(order.getId());
            purchaseItemMapper.insert(item);

            // 更新商品库存
            Product product = productService.getById(item.getProductId());
            if (product == null) {
                throw new BizException("商品不存在，ID: " + item.getProductId());
            }
            int beforeStock = product.getStock();
            int afterStock = beforeStock + item.getQuantity();
            product.setStock(afterStock);
            productService.updateById(product);

            // 记录库存流水
            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setType(1); // 入库
            log.setQuantity(item.getQuantity());
            log.setBeforeStock(beforeStock);
            log.setAfterStock(afterStock);
            log.setRefType("PURCHASE");
            log.setRefOrderId(order.getId());
            inventoryLogService.save(log);
        }

        // 生成支出记录
        FinanceRecord record = new FinanceRecord();
        record.setType(2); // 支出
        record.setAmount(totalAmount);
        record.setRefType("PURCHASE");
        record.setRefOrderId(order.getId());
        record.setPaymentStatus(0);
        record.setRemark("采购单: " + orderNo);
        financeRecordService.save(record);
    }

    /** 结算采购单，并同步更新关联财务记录状态。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(Long id) {
        PurchaseOrder order = getById(id);
        if (order == null) {
            throw new BizException("采购单不存在");
        }
        if (order.getPaymentStatus() == 1) {
            throw new BizException("该采购单已结算");
        }
        order.setPaymentStatus(1);
        updateById(order);

        // 同步更新收支记录
        financeRecordService.lambdaUpdate()
                .eq(FinanceRecord::getRefType, "PURCHASE")
                .eq(FinanceRecord::getRefOrderId, id)
                .set(FinanceRecord::getPaymentStatus, 1)
                .update();
    }

    /**
     * 创建采购单前先校验基础字段，避免进入库存和财务流程后才发现请求不合法。
     */
    private void validateCreateOrder(PurchaseOrder order) {
        if (order.getSupplierId() == null) {
            throw new BizException("供应商不能为空");
        }
        if (order.getOrderDate() == null) {
            throw new BizException("采购日期不能为空");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BizException("采购明细不能为空");
        }

        Supplier supplier = supplierService.getById(order.getSupplierId());
        if (supplier == null) {
            throw new BizException("供应商不存在");
        }
        if (supplier.getAuditStatus() == null || supplier.getAuditStatus() != 1) {
            throw new BizException("仅支持向审核通过的供应商创建采购单");
        }

        for (PurchaseItem item : order.getItems()) {
            if (item.getProductId() == null) {
                throw new BizException("采购商品不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException("采购数量必须大于0");
            }
        }
    }

    /**
     * 将供应商供货清单转成按商品索引的映射，便于校验采购明细是否合法并拿到统一供货价。
     */
    private Map<Long, SupplierProduct> resolveSupplierProductMap(Long supplierId, List<PurchaseItem> items) {
        List<SupplierProduct> supplierProducts = supplierProductService.listBySupplierId(supplierId);
        Map<Long, SupplierProduct> supplierProductMap = new HashMap<>();
        for (SupplierProduct supplierProduct : supplierProducts) {
            supplierProductMap.put(supplierProduct.getProductId(), supplierProduct);
        }

        for (PurchaseItem item : items) {
            SupplierProduct supplierProduct = supplierProductMap.get(item.getProductId());
            if (supplierProduct == null) {
                throw new BizException("所选供应商未维护该商品的供货信息，请先在供应商商品中维护");
            }
            if (supplierProduct.getSupplyPrice() == null) {
                throw new BizException("所选供应商商品未维护供货价，请先完善供应商商品信息");
            }
        }
        return supplierProductMap;
    }
}
