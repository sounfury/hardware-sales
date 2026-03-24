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
@TableName("sales_order")
public class SalesOrder extends BaseEntity {

    private String orderNo;
    private String customerName;
    private String customerPhone;
    private BigDecimal totalAmount;
    private Integer paymentStatus;
    private LocalDate orderDate;
    private String remark;

    @TableField(exist = false)
    private List<SalesItem> items;
}
