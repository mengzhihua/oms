package com.oms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_sales_order_item")
public class SalesOrderItem extends BaseEntity {
    private String orderNo;
    private String sku;
    private String productName;
    private String bundleSku;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer reservedQty;
    private Integer shippedQty;
}
