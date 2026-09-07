package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_warehouse")
public class Warehouse extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private String wmsCode;
    private String province;
    private String city;
    private String address;
    private String contact;
    private String phone;
    private Integer priority;
    private Integer status;
}
