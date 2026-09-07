package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_routing_rule")
public class RoutingRule extends BaseEntity {
    private String name;
    private Integer priority;
    private String channelCode;
    private String shopCode;
    private String province;
    private String sku;
    private String warehouseCode;
    private String carrierCode;
    private Integer status;
}
