package com.hardware.sales.service.dto;

import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class AuthLoginRequest {

    /** 登录用户名。 */
    private String username;

    /** 登录密码。 */
    private String password;
}
