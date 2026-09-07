package com.oms.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_refund")
public class Refund extends BaseEntity {
    private String refundNo;
    private String returnNo;
    private String orderNo;
    private BigDecimal amount;
    private String method;
    private String status;
    private LocalDateTime paidAt;
    private String remark;
    private String operator;
}
