package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_bundle_item")
public class BundleItem extends BaseEntity {
    private String bundleSku;
    private String sku;
    private Integer qty;
}
