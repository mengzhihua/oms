package com.oms.integration.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.oms.aftersale.entity.ReturnOrder;
import com.oms.aftersale.service.AfterSaleService;
import com.oms.common.BizException;
import com.oms.common.R;
import com.oms.integration.service.IntegrationService;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.mapper.InventoryMapper;
import com.oms.inventory.service.InventoryService;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import com.oms.order.mapper.SalesOrderItemMapper;
import com.oms.order.mapper.SalesOrderMapper;
import com.oms.order.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 对外开放接口（X-Api-Key 鉴权，见 AuthInterceptor）：
 * - 渠道：下单、查询订单、查询可售库存
 * - WMS：库存同步、发货回传、退货入库回传
 * - TMS：签收回传、轨迹事件
 */
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class OpenApiController {
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final AfterSaleService afterSaleService;
    private final IntegrationService integrationService;
    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final InventoryMapper inventoryMapper;
    private final ConcurrentHashMap<String, Object> actionCache = new ConcurrentHashMap<String, Object>();

    // ------------------------------------------------------------ 渠道

    /** 渠道下单（按 shopCode + channelOrderNo 幂等） */
    @PostMapping("/channel/orders")
    public R<SalesOrder> channelOrder(@RequestBody OrderCreateRequest req) {
        if (StringUtils.isBlank(req.getChannelOrderNo())) {
            throw new BizException("channelOrderNo 必填");
        }
        SalesOrder existing = orderService.findByChannelOrderNo(req.getShopCode(), req.getChannelOrderNo());
        if (existing != null) {
            integrationService.logInbound(IntegrationService.CHANNEL, "CREATE_ORDER", existing.getOrderNo(), req, true, "重复推送，返回已有订单");
            return R.ok(existing);
        }
        req.setSource("API");
        try {
            SalesOrder o = orderService.create(req);
            integrationService.logInbound(IntegrationService.CHANNEL, "CREATE_ORDER", o.getOrderNo(), req, true, null);
            return R.ok(o);
        } catch (RuntimeException e) {
            integrationService.logInbound(IntegrationService.CHANNEL, "CREATE_ORDER", req.getChannelOrderNo(), req, false, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/channel/orders/{shopCode}/{channelOrderNo}")
    public R<SalesOrder> channelOrderQuery(@PathVariable String shopCode, @PathVariable String channelOrderNo) {
        SalesOrder o = orderService.findByChannelOrderNo(shopCode, channelOrderNo);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        return R.ok(o);
    }

    @Data
    public static class ChannelCancelRequest {
        private String shopCode;
        private String channelOrderNo;
        private String reason;
    }

    @PostMapping("/channel/orders/cancel")
    public R<SalesOrder> channelCancel(@RequestBody ChannelCancelRequest req) {
        SalesOrder o = orderService.findByChannelOrderNo(req.getShopCode(), req.getChannelOrderNo());
        if (o == null) {
            throw new BizException("订单不存在");
        }
        SalesOrder r = orderService.cancel(o.getOrderNo(), StringUtils.isBlank(req.getReason()) ? "渠道取消" : req.getReason());
        integrationService.logInbound(IntegrationService.CHANNEL, "CANCEL_ORDER", o.getOrderNo(), req, true, null);
        return R.ok(r);
    }

    /** 渠道可售库存 */
    @GetMapping("/channel/inventory")
    public R<List<Map<String, Object>>> channelInventory(@RequestParam String shopCode, @RequestParam String skus) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String sku : skus.split(",")) {
            if (StringUtils.isBlank(sku)) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sku", sku.trim());
            m.put("qty", inventoryService.channelAvailable(shopCode, sku.trim()));
            out.add(m);
        }
        return R.ok(out);
    }

    // ------------------------------------------------------------ WMS

    @Data
    public static class StockLine {
        private String warehouseCode;
        private String sku;
        private Integer qty;
    }

    /** WMS 库存全量/增量同步（qty 为在库绝对值） */
    @PostMapping("/wms/inventory")
    public R<List<Inventory>> wmsInventory(@RequestBody List<StockLine> lines) {
        List<Inventory> out = new ArrayList<>();
        for (StockLine l : lines) {
            if (StringUtils.isBlank(l.getWarehouseCode()) || StringUtils.isBlank(l.getSku()) || l.getQty() == null) {
                throw new BizException("warehouseCode/sku/qty 必填");
            }
            out.add(inventoryService.sync(l.getWarehouseCode(), l.getSku(), l.getQty(), "WMS-SYNC"));
        }
        integrationService.logInbound(IntegrationService.WMS, "SYNC_INVENTORY", null, lines, true, null);
        return R.ok(out);
    }

    @Data
    public static class ShippedCallback {
        private String orderNo;
        private String wmsOrderNo;
        private String carrierCode;
        private String trackingNo;
        /** sku -> 实发数量 */
        private Map<String, Integer> items;
    }

    @PostMapping("/wms/shipped")
    public R<SalesOrder> wmsShipped(@RequestBody ShippedCallback cb) {
        if (StringUtils.isBlank(cb.getOrderNo()) || StringUtils.isBlank(cb.getTrackingNo())) {
            throw new BizException("orderNo/trackingNo 必填");
        }
        try {
            SalesOrder o = orderService.shipped(cb.getOrderNo(), cb.getCarrierCode(), cb.getTrackingNo(), cb.getWmsOrderNo(), cb.getItems());
            integrationService.logInbound(IntegrationService.WMS, "SHIPPED", cb.getOrderNo(), cb, true, null);
            return R.ok(o);
        } catch (RuntimeException e) {
            integrationService.logInbound(IntegrationService.WMS, "SHIPPED", cb.getOrderNo(), cb, false, e.getMessage());
            throw e;
        }
    }

    @Data
    public static class ReturnReceivedCallback {
        private String returnNo;
        private String warehouseCode;
        private Map<String, Integer> items;
    }

    @PostMapping("/wms/return-received")
    public R<ReturnOrder> wmsReturnReceived(@RequestBody ReturnReceivedCallback cb) {
        try {
            ReturnOrder r = afterSaleService.receive(cb.getReturnNo(), cb.getItems(), cb.getWarehouseCode());
            integrationService.logInbound(IntegrationService.WMS, "RETURN_RECEIVED", cb.getReturnNo(), cb, true, null);
            return R.ok(r);
        } catch (RuntimeException e) {
            integrationService.logInbound(IntegrationService.WMS, "RETURN_RECEIVED", cb.getReturnNo(), cb, false, e.getMessage());
            throw e;
        }
    }

    // ------------------------------------------------------------ TMS

    @Data
    public static class TmsCallback {
        private String orderNo;
        private String tmsOrderNo;
        private String trackingNo;
        private String event;
        private String location;
        private String remark;
    }

    @PostMapping("/tms/signed")
    public R<SalesOrder> tmsSigned(@RequestBody TmsCallback cb) {
        try {
            SalesOrder o = orderService.signed(cb.getOrderNo(), StringUtils.isBlank(cb.getRemark()) ? "TMS 签收回传" : cb.getRemark());
            integrationService.logInbound(IntegrationService.TMS, "SIGNED", cb.getOrderNo(), cb, true, null);
            return R.ok(o);
        } catch (RuntimeException e) {
            integrationService.logInbound(IntegrationService.TMS, "SIGNED", cb.getOrderNo(), cb, false, e.getMessage());
            throw e;
        }
    }

    /** 物流轨迹事件，仅记录到订单日志 */
    @PostMapping("/tms/track")
    public R<Void> tmsTrack(@RequestBody TmsCallback cb) {
        SalesOrder o = orderService.get(cb.getOrderNo());
        orderService.log(o, "TRACK", o.getStatus(), o.getStatus(),
                String.join(" ", nvl(cb.getEvent()), nvl(cb.getLocation()), nvl(cb.getRemark())).trim());
        integrationService.logInbound(IntegrationService.TMS, "TRACK", cb.getOrderNo(), cb, true, null);
        return R.ok();
    }

    @Data
    public static class IrAction {
        private String type;
        private String targetKey;
        private String idempotencyKey;
        private Map<String, Object> params;
    }

    /** IR 控制塔按订单号下发协同指令，避免依赖登录会话。 */
    @PostMapping("/ir/actions")
    public R<Object> irAction(@RequestBody IrAction cmd) {
        if (cmd == null || StringUtils.isBlank(cmd.getType()) || StringUtils.isBlank(cmd.getTargetKey())) {
            throw new BizException("type 与 targetKey 必填");
        }
        Map<String, Object> params = cmd.getParams() == null ? new LinkedHashMap<>() : cmd.getParams();
        try {
            Object result = executeOnce(cacheKey(cmd.getType(), cmd.getTargetKey(), cmd.getIdempotencyKey()), () -> {
                if ("OMS_HOLD".equals(cmd.getType())) {
                    return orderService.hold(cmd.getTargetKey(), str(params.get("reason"), "IR控制塔挂起"));
                } else if ("OMS_UNHOLD".equals(cmd.getType())) {
                    return orderService.unhold(cmd.getTargetKey());
                } else if ("OMS_REROUTE_WAREHOUSE".equals(cmd.getType())) {
                    return orderService.reroute(cmd.getTargetKey(), str(params.get("warehouseCode"), null));
                } else if ("OMS_AUTO_PROCESS".equals(cmd.getType())) {
                    return orderService.autoProcess(cmd.getTargetKey());
                } else if ("OMS_CANCEL".equals(cmd.getType())) {
                    return orderService.cancel(cmd.getTargetKey(), str(params.get("reason"), "IR控制塔取消"));
                } else if ("OMS_PRIORITIZE".equals(cmd.getType())) {
                    Integer priority = params.get("priority") == null
                            ? 10 : Integer.parseInt(String.valueOf(params.get("priority")));
                    return orderService.updateRemark(cmd.getTargetKey(),
                            str(params.get("remark"), "IR控制塔加急"), priority);
                }
                throw new BizException("不支持的 IR 指令: " + cmd.getType());
            });
            integrationService.logInbound("IR", cmd.getType(), cmd.getTargetKey(), cmd, true, null);
            return R.ok(result);
        } catch (RuntimeException e) {
            integrationService.logInbound("IR", cmd.getType(), cmd.getTargetKey(), cmd, false, e.getMessage());
            throw e;
        }
    }

    private Object executeOnce(String cacheKey, Supplier<Object> work) {
        if (cacheKey == null) {
            return work.get();
        }
        Object cached = actionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        synchronized (actionCache) {
            cached = actionCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            Object created = work.get();
            actionCache.put(cacheKey, created);
            return created;
        }
    }

    private static String cacheKey(String type, String targetKey, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty() || "null".equals(idempotencyKey)) {
            return null;
        }
        return type + "|" + (targetKey == null ? "" : targetKey) + "|" + idempotencyKey.trim();
    }

    /** IR 控制塔拉取订单 / 库存 / 按 SKU+仓+渠道的日销，避免登录分页和报表口径不一致。 */
    @GetMapping("/ir/snapshots")
    public R<Map<String, Object>> irSnapshots() {
        List<SalesOrder> orders = orderMapper.selectList(null);
        List<SalesOrderItem> items = itemMapper.selectList(null);
        Map<String, Integer> qtyByOrder = new HashMap<String, Integer>();
        Map<String, String> skuByOrder = new HashMap<String, String>();
        Map<String, SalesOrder> orderByNo = new LinkedHashMap<String, SalesOrder>();
        for (SalesOrder order : orders) {
            orderByNo.put(order.getOrderNo(), order);
        }
        for (SalesOrderItem item : items) {
            int qty = item.getQty() == null ? 0 : item.getQty();
            qtyByOrder.put(item.getOrderNo(), qtyByOrder.getOrDefault(item.getOrderNo(), 0) + qty);
            if (item.getSku() != null && !item.getSku().trim().isEmpty()
                    && !skuByOrder.containsKey(item.getOrderNo())) {
                skuByOrder.put(item.getOrderNo(), item.getSku().trim());
            }
        }
        List<Map<String, Object>> orderRows = new ArrayList<Map<String, Object>>();
        for (SalesOrder order : orders) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("orderNo", order.getOrderNo());
            row.put("channelCode", order.getChannelCode());
            row.put("shopCode", order.getShopCode());
            row.put("warehouseCode", order.getWarehouseCode());
            row.put("province", order.getProvince());
            row.put("city", order.getCity());
            row.put("status", order.getStatus());
            row.put("priority", order.getPriority() == null ? 0 : order.getPriority());
            row.put("payAmount", order.getPayAmount());
            row.put("freight", order.getFreight());
            String sku = skuByOrder.get(order.getOrderNo());
            row.put("sku", sku);
            row.put("skuCode", sku);
            row.put("qty", qtyByOrder.getOrDefault(order.getOrderNo(), 0));
            row.put("orderTime", order.getOrderTime());
            row.put("payTime", order.getPayTime());
            row.put("shipTime", order.getShippedAt());
            row.put("completeTime", order.getCompletedAt());
            row.put("carrierCode", order.getCarrierCode());
            row.put("trackingNo", order.getTrackingNo());
            row.put("wmsOrderNo", order.getWmsOrderNo());
            row.put("tmsOrderNo", order.getTmsOrderNo());
            orderRows.add(row);
        }
        List<Map<String, Object>> inventoryRows = new ArrayList<Map<String, Object>>();
        for (Inventory inventory : inventoryMapper.selectList(null)) {
            int onHand = n(inventory.getQtyOnHand());
            int reserved = n(inventory.getQtyReserved());
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("warehouseCode", inventory.getWarehouseCode());
            row.put("sku", inventory.getSku());
            row.put("qtyOnHand", onHand);
            row.put("qtyReserved", reserved);
            row.put("qtyAvailable", onHand - reserved);
            row.put("safetyQty", n(inventory.getSafetyQty()));
            inventoryRows.add(row);
        }
        Map<String, Map<String, Object>> salesByKey = new LinkedHashMap<String, Map<String, Object>>();
        for (SalesOrderItem item : items) {
            SalesOrder order = orderByNo.get(item.getOrderNo());
            if (order == null || order.getOrderTime() == null
                    || "CANCELLED".equals(order.getStatus()) || "SPLIT".equals(order.getStatus())) {
                continue;
            }
            LocalDate day = order.getOrderTime().toLocalDate();
            String sku = item.getSku() == null ? "" : item.getSku();
            String warehouse = order.getWarehouseCode() == null ? "" : order.getWarehouseCode();
            String channel = order.getChannelCode() == null ? "" : order.getChannelCode();
            String key = day + "|" + sku + "|" + warehouse + "|" + channel;
            Map<String, Object> row = salesByKey.get(key);
            if (row == null) {
                row = new LinkedHashMap<String, Object>();
                row.put("salesDate", day.toString());
                row.put("sku", sku);
                row.put("warehouseCode", warehouse);
                row.put("channelCode", channel);
                row.put("qty", BigDecimal.ZERO);
                row.put("amount", BigDecimal.ZERO);
                salesByKey.put(key, row);
            }
            BigDecimal qty = item.getQty() == null ? BigDecimal.ZERO : BigDecimal.valueOf(item.getQty());
            BigDecimal amount = item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
            row.put("qty", ((BigDecimal) row.get("qty")).add(qty));
            row.put("amount", ((BigDecimal) row.get("amount")).add(amount));
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("system", "OMS");
        payload.put("orders", orderRows);
        payload.put("inventory", inventoryRows);
        payload.put("sales", new ArrayList<Map<String, Object>>(salesByKey.values()));
        return R.ok(payload);
    }

    private static int n(Integer value) {
        return value == null ? 0 : value;
    }

    private static String str(Object value, String fallback) {
        if (value == null || String.valueOf(value).trim().isEmpty() || "null".equals(String.valueOf(value))) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
