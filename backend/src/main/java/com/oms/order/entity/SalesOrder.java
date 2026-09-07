package com.oms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_sales_order")
public class SalesOrder extends BaseEntity {
    private String orderNo;
    private String parentOrderNo;
    private String channelCode;
    private String shopCode;
    private String channelOrderNo;
    private String source;
    private String customerCode;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String address;
    private String status;
    private String payStatus;
    private Integer priority;
    private LocalDateTime orderTime;
    private LocalDateTime payTime;
    private BigDecimal goodsAmount;
    private BigDecimal freight;
    private BigDecimal discount;
    private BigDecimal payAmount;
    private String warehouseCode;
    private String carrierCode;
    private String trackingNo;
    private String wmsOrderNo;
    private String tmsOrderNo;
    private String buyerRemark;
    private String sellerRemark;
    private String holdReason;
    private String cancelReason;
    private String auditedBy;
    private LocalDateTime auditedAt;
    private LocalDateTime pushedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime signedAt;
    private LocalDateTime completedAt;
}
