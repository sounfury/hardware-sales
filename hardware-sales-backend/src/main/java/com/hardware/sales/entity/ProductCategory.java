package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_category")
public class ProductCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer sort;
    /** 0-停用 1-启用 */
    private Integer status;

    @TableField(exist = false)
    private Long productCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
