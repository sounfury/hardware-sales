package com.hardware.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier")
public class Supplier extends BaseEntity {

    private Long userId;
    private String companyName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String businessScope;
    /** 0-待审核 1-通过 2-驳回 */
    private Integer auditStatus;
    private String auditRemark;
}
