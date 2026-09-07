package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_product")
public class Product extends BaseEntity {
    private String sku;
    private String name;
    private String type;
    private String category;
    private String spec;
    private String unit;
    private String barcode;
    private BigDecimal price;
    private BigDecimal weightKg;
    private Integer status;
}
