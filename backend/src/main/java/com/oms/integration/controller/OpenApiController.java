package com.oms.integration.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.oms.aftersale.entity.ReturnOrder;
import com.oms.aftersale.service.AfterSaleService;
import com.oms.common.BizException;
import com.oms.common.R;
import com.oms.integration.service.IntegrationService;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.service.InventoryService;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.SalesOrder;
import com.oms.order.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
