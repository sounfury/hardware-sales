package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品实体，除基础价格库存外，也负责记录补货阈值和当前补货状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    private Long categoryId;
    private String name;
    private String brand;
    private String spec;
    /** 商品描述/作用 */
    private String description;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer stock;
    /** 库存小于等于该值时，前端按低库存展示 */
    private Integer restockThreshold;
    /** 0-正常 1-补货中 */
    private Integer restockStatus;
    /** 当前跟进补货的供应商ID */
    private Long restockSupplierId;
}
