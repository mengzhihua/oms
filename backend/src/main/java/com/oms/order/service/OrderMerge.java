package com.oms.order.service;

import com.oms.order.entity.SalesOrder;
import java.time.Duration;
import java.time.LocalDateTime;

/** 同一客户、同一收货地址、同一时段内的待处理订单可以合到一张单上。 */
public final class OrderMerge {
    public static final int DEFAULT_MINUTES = 30;

    private OrderMerge() {}

    public static boolean canMerge(SalesOrder target, SalesOrder other, int minutes) {
        if (target == null || other == null || minutes < 0) {
            return false;
        }
        if (target.getOrderNo() != null && target.getOrderNo().equals(other.getOrderNo())) {
            return false;
        }
        if (!open(target.getStatus()) || !open(other.getStatus())) {
            return false;
        }
        if (!same(target.getCustomerCode(), other.getCustomerCode()) || blank(target.getCustomerCode())) {
            return false;
        }
        if (!same(address(target), address(other))) {
            return false;
        }
        if (!blank(target.getWarehouseCode())
                && !blank(other.getWarehouseCode())
                && !same(target.getWarehouseCode(), other.getWarehouseCode())) {
            return false;
        }
        LocalDateTime left = when(target);
        LocalDateTime right = when(other);
        if (left == null || right == null) {
            return false;
        }
        return Math.abs(Duration.between(left, right).toMinutes()) <= minutes;
    }

    private static boolean open(String status) {
        return "CREATED".equals(status) || "AUDITED".equals(status);
    }

    private static LocalDateTime when(SalesOrder order) {
        return order.getOrderTime() != null ? order.getOrderTime() : order.getCreatedAt();
    }

    private static String address(SalesOrder order) {
        return text(order.getProvince())
                + text(order.getCity())
                + text(order.getDistrict())
                + text(order.getAddress());
    }

    private static boolean same(String left, String right) {
        return text(left).equals(text(right));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
