package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_channel")
public class Channel extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private String remark;
    private Integer status;
}
