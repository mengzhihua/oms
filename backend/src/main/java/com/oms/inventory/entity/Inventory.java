package com.oms.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_inventory")
public class Inventory extends BaseEntity {
    private String warehouseCode;
    private String sku;
    private Integer qtyOnHand;
    private Integer qtyReserved;
    private Integer safetyQty;
}
