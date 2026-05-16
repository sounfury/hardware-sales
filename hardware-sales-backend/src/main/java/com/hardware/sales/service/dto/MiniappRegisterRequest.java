package com.hardware.sales.service.dto;

import lombok.Data;

/**
 * 微信小程序注册请求参数。
 */
@Data
public class MiniappRegisterRequest {

    /** 注册类型：SUPPLIER-供应商 CUSTOMER-客户。 */
    private String registerType;

    /** 登录用户名。 */
    private String username;

    /** 登录密码。 */
    private String password;

    /** 昵称。 */
    private String nickname;

    /** 联系电话。 */
    private String phone;
}
