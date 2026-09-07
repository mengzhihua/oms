package com.oms.aftersale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnCreateRequest {
    private String orderNo;
    /** RETURN / REFUND_ONLY / EXCHANGE */
    private String type;
    private String reason;
    private String remark;
    private BigDecimal refundAmount;
    private String warehouseCode;
    private String returnTrackingNo;
    private List<Item> items;

    @Data
    public static class Item {
        private String sku;
        private Integer qty;
        private String exchangeSku;
    }
}
