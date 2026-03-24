package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String openid;
    private String nickname;
    private String avatar;
    private String phone;
    /** ADMIN-系统管理员 BUSINESS-业务管理员 SUPPLIER-供应商 */
    private String role;
    /** 0-禁用 1-正常 */
    private Integer status;
}
