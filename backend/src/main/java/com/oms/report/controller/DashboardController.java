package com.oms.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.aftersale.entity.ReturnOrder;
import com.oms.aftersale.mapper.ReturnOrderMapper;
import com.oms.common.R;
import com.oms.integration.entity.IntegrationLog;
import com.oms.integration.mapper.IntegrationLogMapper;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.mapper.InventoryMapper;
import com.oms.inventory.service.InventoryService;
import com.oms.order.entity.SalesOrder;
import com.oms.order.mapper.SalesOrderMapper;
import com.oms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** OMS 工作台汇总 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SalesOrderMapper orderMapper;
    private final ReturnOrderMapper returnMapper;
    private final InventoryMapper inventoryMapper;
    private final IntegrationLogMapper integrationLogMapper;
    private final JdbcTemplate jdbc;

    @GetMapping
    public R<Map<String, Object>> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime today = LocalDate.now().atStartOfDay();

        List<SalesOrder> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                .ge(SalesOrder::getCreatedAt, today).ne(SalesOrder::getStatus, OrderService.SPLIT));
        m.put("todayOrders", todayOrders.size());
        m.put("todayAmount", todayOrders.stream().filter(o -> !OrderService.CANCELLED.equals(o.getStatus()))
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
        m.put("todayShipped", orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().ge(SalesOrder::getShippedAt, today)));

        Map<String, Long> status = new LinkedHashMap<>();
        for (String s : Arrays.asList(OrderService.CREATED, OrderService.HOLD, OrderService.AUDITED, OrderService.ALLOCATED,
                OrderService.PUSHED, OrderService.SHIPPED, OrderService.COMPLETED, OrderService.CANCELLED)) {
            status.put(s, orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getStatus, s)));
        }
        m.put("orderStatus", status);
        m.put("pendingAudit", status.get(OrderService.CREATED));
        m.put("pendingAllocate", status.get(OrderService.AUDITED));
        m.put("pendingPush", status.get(OrderService.ALLOCATED));
        m.put("pendingShip", status.get(OrderService.PUSHED));
        m.put("hold", status.get(OrderService.HOLD));
        m.put("inTransit", status.get(OrderService.SHIPPED));

        m.put("pendingAfterSale", returnMapper.selectCount(new LambdaQueryWrapper<ReturnOrder>()
                .in(ReturnOrder::getStatus, "CREATED", "AUDITED", "RECEIVED")));
        m.put("integrationFailed", integrationLogMapper.selectCount(new LambdaQueryWrapper<IntegrationLog>()
                .eq(IntegrationLog::getSuccess, 0).ge(IntegrationLog::getCreatedAt, today.minusDays(7))));

        List<Inventory> inv = inventoryMapper.selectList(null);
        m.put("skuInStock", inv.stream().filter(i -> InventoryService.available(i) > 0).map(Inventory::getSku).distinct().count());
        m.put("qtyOnHand", inv.stream().mapToInt(i -> i.getQtyOnHand() == null ? 0 : i.getQtyOnHand()).sum());
        m.put("qtyReserved", inv.stream().mapToInt(i -> i.getQtyReserved() == null ? 0 : i.getQtyReserved()).sum());
        m.put("lowStock", inv.stream().filter(i -> InventoryService.available(i) <= (i.getSafetyQty() == null ? 0 : i.getSafetyQty())).count());

        m.put("channelToday", jdbc.queryForList(
                "SELECT channel_code AS channel_code, COUNT(*) AS orders, COALESCE(SUM(pay_amount),0) AS amount FROM oms_sales_order "
                        + "WHERE created_at >= ? AND status <> 'SPLIT' AND status <> 'CANCELLED' GROUP BY channel_code ORDER BY orders DESC", today));
        m.put("trend", jdbc.queryForList(
                "SELECT CAST(created_at AS DATE) AS order_day, COUNT(*) AS orders, COALESCE(SUM(pay_amount),0) AS amount FROM oms_sales_order "
                        + "WHERE created_at >= ? AND status <> 'SPLIT' GROUP BY CAST(created_at AS DATE) ORDER BY order_day", today.minusDays(6)));
        return R.ok(m);
    }
}
