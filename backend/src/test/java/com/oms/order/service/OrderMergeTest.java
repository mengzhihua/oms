package com.oms.order.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oms.order.entity.SalesOrder;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OrderMergeTest {
    @Test
    void sameCustomerAndAddressWithinWindowCanMerge() {
        SalesOrder target = order("SO-1", "CUST-1", "CREATED", LocalDateTime.of(2026, 9, 23, 10, 0));
        SalesOrder peer = order("SO-2", "CUST-1", "AUDITED", LocalDateTime.of(2026, 9, 23, 10, 20));
        assertTrue(OrderMerge.canMerge(target, peer, OrderMerge.DEFAULT_MINUTES));
    }

    @Test
    void differentAddressWarehouseOrLateOrderStaysSeparate() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 23, 10, 0);
        SalesOrder target = order("SO-1", "CUST-1", "CREATED", at);
        SalesOrder otherAddress = order("SO-2", "CUST-1", "CREATED", at);
        otherAddress.setAddress("另一条路");
        SalesOrder otherWarehouse = order("SO-3", "CUST-1", "CREATED", at);
        target.setWarehouseCode("WH01");
        otherWarehouse.setWarehouseCode("WH02");
        SalesOrder late = order("SO-4", "CUST-1", "CREATED", at.plusMinutes(31));
        SalesOrder shipped = order("SO-5", "CUST-1", "SHIPPED", at);
        assertFalse(OrderMerge.canMerge(target, otherAddress, 30));
        assertFalse(OrderMerge.canMerge(target, otherWarehouse, 30));
        assertFalse(OrderMerge.canMerge(target, late, 30));
        assertFalse(OrderMerge.canMerge(target, shipped, 30));
        assertFalse(OrderMerge.canMerge(target, order("SO-6", "", "CREATED", at), 30));
    }

    private static SalesOrder order(String no, String customer, String status, LocalDateTime time) {
        SalesOrder order = new SalesOrder();
        order.setOrderNo(no);
        order.setCustomerCode(customer);
        order.setStatus(status);
        order.setProvince("江苏");
        order.setCity("苏州");
        order.setAddress("工业园");
        order.setOrderTime(time);
        return order;
    }
}
