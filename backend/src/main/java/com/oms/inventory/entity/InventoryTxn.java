package com.oms.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_inventory_txn")
public class InventoryTxn extends BaseEntity {
    private String warehouseCode;
    private String sku;
    private String type;
    private Integer qty;
    private Integer onHandAfter;
    private Integer reservedAfter;
    private String refNo;
    private String remark;
    private String operator;
}
