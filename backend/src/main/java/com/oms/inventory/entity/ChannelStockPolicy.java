package com.oms.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_channel_stock_policy")
public class ChannelStockPolicy extends BaseEntity {
    private String shopCode;
    private String sku;
    private String warehouseCode;
    private String mode;
    private Integer ratio;
    private Integer fixedQty;
    private Integer status;
}
