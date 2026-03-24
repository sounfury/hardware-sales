package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("finance_record")
public class FinanceRecord extends BaseEntity {

    /** 1-收入(销售) 2-支出(采购) */
    private Integer type;
    private BigDecimal amount;
    /** PURCHASE-采购 SALES-销售 */
    private String refType;
    private Long refOrderId;
    /** 0-未结算 1-已结算 */
    private Integer paymentStatus;
    private String remark;
}
