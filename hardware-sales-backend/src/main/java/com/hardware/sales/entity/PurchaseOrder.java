package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_order")
public class PurchaseOrder extends BaseEntity {

    private String orderNo;
    private Long supplierId;
    private BigDecimal totalAmount;
    private Integer paymentStatus;
    private LocalDate orderDate;
    private String remark;

    @TableField(exist = false)
    private List<PurchaseItem> items;

    @TableField(exist = false)
    private String supplierName;

    /** 是否在创建后自动结算，仅作为前后端交互字段使用。 */
    @TableField(exist = false)
    private Boolean autoSettle;
}
