package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_log")
public class InventoryLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    /** 1-入库 2-出库 */
    private Integer type;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    /** PURCHASE-采购 SALES-销售 */
    private String refType;
    private Long refOrderId;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String productName;
}
