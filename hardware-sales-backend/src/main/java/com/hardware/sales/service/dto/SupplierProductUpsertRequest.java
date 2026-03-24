package com.hardware.sales.service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SupplierProductUpsertRequest {

    private Long id;
    private Long productId;
    private BigDecimal supplyPrice;
    private String remark;
}
