package com.oms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_log")
public class OrderLog extends BaseEntity {
    private String orderNo;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String remark;
}
