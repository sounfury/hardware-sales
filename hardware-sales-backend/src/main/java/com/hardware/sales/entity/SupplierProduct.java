package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_product")
public class SupplierProduct extends BaseEntity {

    private Long supplierId;
    private Long productId;
    private BigDecimal supplyPrice;
    private String remark;

    @TableField(exist = false)
    private Long supplierUserId;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productSpec;

    @TableField(exist = false)
    private String productUnit;
}
