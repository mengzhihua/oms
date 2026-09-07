package com.oms.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 创建销售订单（手工录入 / 渠道 Open API / 导入 共用） */
@Data
public class OrderCreateRequest {
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
    private String payStatus;
    private Integer priority;
    private LocalDateTime orderTime;
    private LocalDateTime payTime;
    private BigDecimal freight;
    private BigDecimal discount;
    private BigDecimal payAmount;
    private String buyerRemark;
    private String sellerRemark;
    private List<Item> items;

    @Data
    public static class Item {
        private String sku;
        private Integer qty;
        private BigDecimal price;
    }
}
