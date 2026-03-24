package com.hardware.sales.service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpsertRequest {

    private Long id;
    private Long categoryId;
    private String name;
    private String brand;
    private String spec;
    private String description;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer restockThreshold;
}
