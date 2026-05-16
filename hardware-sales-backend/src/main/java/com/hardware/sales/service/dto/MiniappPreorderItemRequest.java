package com.hardware.sales.service.dto;

import lombok.Data;

/**
 * 小程序客户预定明细请求参数。
 */
@Data
public class MiniappPreorderItemRequest {

    /** 商品 ID。 */
    private Long productId;

    /** 预定数量。 */
    private Integer quantity;
}
