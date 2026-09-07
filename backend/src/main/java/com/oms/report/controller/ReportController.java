package com.oms.report.controller;

import com.oms.common.Csv;
import com.oms.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 运营报表（SQL 聚合） */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final JdbcTemplate jdbc;

    /** 每日订单量 / 销售额 / 发货量 */
    @GetMapping("/order-daily")
    public R<List<Map<String, Object>>> orderDaily(@RequestParam(defaultValue = "30") int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return R.ok(jdbc.queryForList(
                "SELECT CAST(order_time AS DATE) AS order_day, COUNT(*) AS orders, "
                        + "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled, "
                        + "SUM(CASE WHEN shipped_at IS NOT NULL THEN 1 ELSE 0 END) AS shipped, "
                        + "COALESCE(SUM(CASE WHEN status<>'CANCELLED' THEN pay_amount ELSE 0 END),0) AS amount "
                        + "FROM oms_sales_order WHERE status <> 'SPLIT' AND order_time >= ? GROUP BY CAST(order_time AS DATE) ORDER BY order_day",
                from.atStartOfDay()));
    }

    /** 渠道 / 店铺销售汇总 */
    @GetMapping("/channel-sales")
    public R<List<Map<String, Object>>> channelSales(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(29) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        return R.ok(jdbc.queryForList(
                "SELECT channel_code AS channel_code, shop_code AS shop_code, COUNT(*) AS orders, "
                        + "SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) AS cancelled, "
                        + "COALESCE(SUM(CASE WHEN status<>'CANCELLED' THEN pay_amount ELSE 0 END),0) AS amount "
                        + "FROM oms_sales_order WHERE status <> 'SPLIT' AND order_time >= ? AND order_time < ? "
                        + "GROUP BY channel_code, shop_code ORDER BY amount DESC",
                f.atStartOfDay(), t.plusDays(1).atStartOfDay()));
    }

    /** SKU 销量排行 */
    @GetMapping("/sku-sales")
    public R<List<Map<String, Object>>> skuSales(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "50") int limit) {
        return R.ok(jdbc.queryForList(
                "SELECT i.sku, MAX(i.product_name) AS product_name, SUM(i.qty) AS qty, COALESCE(SUM(i.amount),0) AS amount, COUNT(DISTINCT i.order_no) AS orders "
                        + "FROM oms_sales_order_item i JOIN oms_sales_order o ON o.order_no = i.order_no "
                        + "WHERE o.status NOT IN ('CANCELLED','SPLIT') AND o.order_time >= ? GROUP BY i.sku ORDER BY qty DESC LIMIT " + Math.max(1, Math.min(limit, 500)),
                LocalDate.now().minusDays(days - 1L).atStartOfDay()));
    }

    /** 仓库发货统计 */
    @GetMapping("/warehouse-ship")
    public R<List<Map<String, Object>>> warehouseShip(@RequestParam(defaultValue = "30") int days) {
        return R.ok(jdbc.queryForList(
                "SELECT warehouse_code AS warehouse_code, COUNT(*) AS orders, "
                        + "SUM(CASE WHEN status IN ('SHIPPED','COMPLETED') THEN 1 ELSE 0 END) AS shipped, "
                        + "SUM(CASE WHEN status IN ('ALLOCATED','PUSHED') THEN 1 ELSE 0 END) AS pending "
                        + "FROM oms_sales_order WHERE warehouse_code IS NOT NULL AND status <> 'SPLIT' AND order_time >= ? GROUP BY warehouse_code ORDER BY orders DESC",
                LocalDate.now().minusDays(days - 1L).atStartOfDay()));
    }

    /** 售后统计（按类型/状态） */
    @GetMapping("/aftersale")
    public R<List<Map<String, Object>>> afterSale(@RequestParam(defaultValue = "30") int days) {
        return R.ok(jdbc.queryForList(
                "SELECT type, status, COUNT(*) AS cnt, COALESCE(SUM(refund_amount),0) AS refund_amount FROM oms_return_order "
                        + "WHERE created_at >= ? GROUP BY type, status ORDER BY type, status",
                LocalDate.now().minusDays(days - 1L).atStartOfDay()));
    }

    /** 订单时效：审核/推送/发货/签收平均耗时（小时） */
    @GetMapping("/lead-time")
    public R<Map<String, Object>> leadTime(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT created_at, audited_at, pushed_at, shipped_at, signed_at FROM oms_sales_order WHERE status <> 'SPLIT' AND created_at >= ?",
                LocalDate.now().minusDays(days - 1L).atStartOfDay());
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("orders", rows.size());
        m.put("audit_hours", avgHours(rows, "created_at", "audited_at"));
        m.put("push_hours", avgHours(rows, "audited_at", "pushed_at"));
        m.put("ship_hours", avgHours(rows, "pushed_at", "shipped_at"));
        m.put("deliver_hours", avgHours(rows, "shipped_at", "signed_at"));
        return R.ok(m);
    }

    private static Double avgHours(List<Map<String, Object>> rows, String from, String to) {
        double sum = 0;
        int n = 0;
        for (Map<String, Object> r : rows) {
            Object a = r.get(from);
            Object b = r.get(to);
            if (a instanceof java.sql.Timestamp && b instanceof java.sql.Timestamp) {
                sum += (((java.sql.Timestamp) b).getTime() - ((java.sql.Timestamp) a).getTime()) / 3600000.0;
                n++;
            }
        }
        return n == 0 ? null : Math.round(sum / n * 100) / 100.0;
    }

    @GetMapping("/sku-sales/export")
    public ResponseEntity<byte[]> skuSalesExport(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> rows = skuSales(days, 500).getData();
        return Csv.download("sku_sales.csv", new String[]{"SKU", "商品", "销量", "销售额", "订单数"},
                rows, r -> new Object[]{r.get("sku"), r.get("product_name"), r.get("qty"), r.get("amount"), r.get("orders")});
    }
}
