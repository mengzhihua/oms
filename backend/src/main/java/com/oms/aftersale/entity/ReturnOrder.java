package com.oms.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_return_order")
public class ReturnOrder extends BaseEntity {
    private String returnNo;
    private String orderNo;
    private String type;
    private String status;
    private String channelCode;
    private String shopCode;
    private String customerCode;
    private String reason;
    private BigDecimal refundAmount;
    private String warehouseCode;
    private String returnTrackingNo;
    private String exchangeOrderNo;
    private String remark;
    private String auditedBy;
    private LocalDateTime auditedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime completedAt;
}
