package com.oms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_shop")
public class Shop extends BaseEntity {
    private String code;
    private String name;
    private String channelCode;
    private String defaultWarehouseCode;
    private Integer autoAudit;
    /** 合单时间窗，分钟。空则用 30。 */
    private Integer mergeMinutes;
    private String contact;
    private String phone;
    private Integer status;
}
