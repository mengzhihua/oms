package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_carrier")
public class Carrier extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private String tmsCode;
    private String contact;
    private String phone;
    private Integer status;
}
