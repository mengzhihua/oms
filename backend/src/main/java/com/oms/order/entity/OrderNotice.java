package com.oms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_notice")
public class OrderNotice extends BaseEntity {
    private String orderNo;
    private String channel;
    private String target;
    private String content;
    private String status;
    private String detail;
}
