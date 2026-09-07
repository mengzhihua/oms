package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_customer")
public class Customer extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private String level;
    private String phone;
    private String email;
    private String province;
    private String city;
    private String address;
    private Integer status;
}
