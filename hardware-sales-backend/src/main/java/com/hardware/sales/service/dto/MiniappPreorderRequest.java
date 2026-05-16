package com.hardware.sales.service.dto;

import lombok.Data;

import java.util.List;

/**
 * 小程序客户预定请求参数。
 */
@Data
public class MiniappPreorderRequest {

    /** 预定商品明细。 */
    private List<MiniappPreorderItemRequest> items;

    /** 订单备注，例如到店自提。 */
    private String remark;
}
