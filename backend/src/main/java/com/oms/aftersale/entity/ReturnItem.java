package com.oms.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_return_item")
public class ReturnItem extends BaseEntity {
    private String returnNo;
    private String sku;
    private String productName;
    private Integer qty;
    private Integer receivedQty;
    private BigDecimal amount;
    private String exchangeSku;
}
