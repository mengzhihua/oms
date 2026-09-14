package com.oms.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.common.BizException;
import com.oms.integration.entity.IntegrationLog;
import com.oms.integration.mapper.IntegrationLogMapper;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 下游系统（WMS / TMS）出站调用、上游渠道系统（DMS）状态回推 + 入站回传的统一日志。
 * mock=true 时不真正外呼，直接模拟成功并生成下游单号，便于本地/演示环境跑通全链路。
 */
@Slf4j
@Service
public class IntegrationService {
    public static final String OUT = "OUT";
    public static final String IN = "IN";
    public static final String WMS = "WMS";
    public static final String TMS = "TMS";
    public static final String CHANNEL = "CHANNEL";
    public static final String DMS = "DMS";

    private final IntegrationLogMapper logMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate rest = new RestTemplate();

    @Value("${oms.integration.wms-url:}")
    private String wmsUrl;
    @Value("${oms.integration.tms-url:}")
    private String tmsUrl;
    @Value("${oms.integration.dms-url:}")
    private String dmsUrl;
    @Value("${oms.integration.dms-key:}")
    private String dmsKey;
    @Value("${oms.integration.dms-channel-code:DMS}")
    private String dmsChannelCode;
    @Value("${oms.integration.mock:true}")
    private boolean mock;

    public IntegrationService(IntegrationLogMapper logMapper, ObjectMapper objectMapper) {
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    public boolean isMock() {
        return mock;
    }

    /** 推送出库单到 WMS，返回 WMS 单号 */
    public String pushOutboundToWms(String orderNo, Map<String, Object> payload) {
        return call(WMS, "PUSH_OUTBOUND", orderNo, wmsUrl + "/api/open/outbound", payload, "WMS-" + orderNo);
    }

    /** 通知 WMS 取消出库单 */
    public void cancelOutboundInWms(String orderNo, String wmsOrderNo) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("wmsOrderNo", wmsOrderNo);
        call(WMS, "CANCEL_OUTBOUND", orderNo, wmsUrl + "/api/open/outbound/cancel", payload, "OK");
    }

    /** 通知 WMS 创建退货入库单 */
    public String pushReturnToWms(String returnNo, Map<String, Object> payload) {
        return call(WMS, "PUSH_RETURN", returnNo, wmsUrl + "/api/open/return", payload, "WMS-RT-" + returnNo);
    }

    /** 在 TMS 创建运输单（发货后），返回 TMS 单号 */
    public String createTransportInTms(String orderNo, Map<String, Object> payload) {
        return call(TMS, "CREATE_TRANSPORT", orderNo, tmsUrl + "/api/open/transport-order", payload, "TMS-" + orderNo);
    }

    public boolean isDmsOrder(SalesOrder o) {
        return o != null && dmsChannelCode.equals(o.getChannelCode());
    }

    /**
     * DMS 渠道订单状态变化(发货/签收/取消)回推 DMS(未配置 dms-url 时仅记日志)。尽力而为:失败只记日志不抛出,
     * 避免回推故障阻断 OMS 本身的发货/签收事务;DMS 侧仍可主动轮询对账。
     */
    public void notifyDms(SalesOrder o, List<SalesOrderItem> items, String event) {
        if (!isDmsOrder(o) || o.getChannelOrderNo() == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("orderNo", o.getOrderNo());
        payload.put("shopCode", o.getShopCode());
        payload.put("channelOrderNo", o.getChannelOrderNo());
        payload.put("status", o.getStatus());
        payload.put("warehouseCode", o.getWarehouseCode());
        payload.put("carrierCode", o.getCarrierCode());
        payload.put("trackingNo", o.getTrackingNo());
        payload.put("wmsOrderNo", o.getWmsOrderNo());
        payload.put("cancelReason", o.getCancelReason());
        List<Map<String, Object>> lines = new java.util.ArrayList<>();
        if (items != null) {
            for (SalesOrderItem it : items) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sku", it.getSku());
                m.put("qty", it.getQty());
                m.put("shippedQty", it.getShippedQty());
                lines.add(m);
            }
        }
        payload.put("items", lines);

        IntegrationLog l = new IntegrationLog();
        l.setDirection(OUT);
        l.setTarget(DMS);
        l.setAction("NOTIFY_" + event);
        l.setRefNo(o.getOrderNo());
        l.setRequestBody(json(payload));
        if (dmsUrl == null || dmsUrl.isEmpty()) {
            l.setSuccess(1);
            l.setResponseBody("{\"mock\":true}");
            logMapper.insert(l);
            return;
        }
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("X-Api-Key", dmsKey);
            String body = rest.postForObject(dmsUrl + "/api/open/oms/orders/status", new HttpEntity<>(payload, h), String.class);
            l.setSuccess(1);
            l.setResponseBody(body);
        } catch (Exception e) {
            l.setSuccess(0);
            l.setErrorMsg(trim(e.getMessage()));
            log.warn("DMS 状态回推失败 {} {}: {}", event, o.getOrderNo(), e.getMessage());
        }
        logMapper.insert(l);
    }

    /** 记录入站回传 */
    public void logInbound(String target, String action, String refNo, Object payload, boolean success, String error) {
        IntegrationLog l = new IntegrationLog();
        l.setDirection(IN);
        l.setTarget(target);
        l.setAction(action);
        l.setRefNo(refNo);
        l.setRequestBody(json(payload));
        l.setSuccess(success ? 1 : 0);
        l.setErrorMsg(trim(error));
        logMapper.insert(l);
    }

    private String call(String target, String action, String refNo, String url, Map<String, Object> payload, String mockResult) {
        IntegrationLog l = new IntegrationLog();
        l.setDirection(OUT);
        l.setTarget(target);
        l.setAction(action);
        l.setRefNo(refNo);
        l.setRequestBody(json(payload));
        String baseUrl = WMS.equals(target) ? wmsUrl : tmsUrl;
        if (mock || baseUrl == null || baseUrl.isEmpty()) {
            l.setSuccess(1);
            l.setResponseBody("{\"mock\":true,\"result\":\"" + mockResult + "\"}");
            logMapper.insert(l);
            return mockResult;
        }
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            String body = rest.postForObject(url, new HttpEntity<>(payload, h), String.class);
            l.setSuccess(1);
            l.setResponseBody(body);
            logMapper.insert(l);
            Map<?, ?> m = objectMapper.readValue(body, Map.class);
            Object data = m.get("data");
            if (data instanceof Map && ((Map<?, ?>) data).get("code") != null) {
                return String.valueOf(((Map<?, ?>) data).get("code"));
            }
            return data == null ? mockResult : String.valueOf(data);
        } catch (Exception e) {
            l.setSuccess(0);
            l.setErrorMsg(trim(e.getMessage()));
            logMapper.insert(l);
            log.warn("{} {} 调用失败: {}", target, action, e.getMessage());
            throw new BizException(target + " 调用失败: " + e.getMessage());
        }
    }

    private String json(Object o) {
        try {
            return o == null ? null : objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return String.valueOf(o);
        }
    }

    private static String trim(String s) {
        return s == null || s.length() <= 500 ? s : s.substring(0, 500);
    }
}
