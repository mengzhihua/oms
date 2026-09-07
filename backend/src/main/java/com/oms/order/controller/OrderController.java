package com.oms.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oms.common.BizException;
import com.oms.common.Csv;
import com.oms.common.R;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.OrderLog;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import com.oms.order.mapper.SalesOrderMapper;
import com.oms.order.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final SalesOrderMapper orderMapper;
    private final OrderService orderService;

    @GetMapping("/page")
    public R<Page<SalesOrder>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "20") long size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String channelCode,
                                    @RequestParam(required = false) String shopCode,
                                    @RequestParam(required = false) String warehouseCode,
                                    @RequestParam(required = false) String source,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LambdaQueryWrapper<SalesOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> w.like(SalesOrder::getOrderNo, keyword).or().like(SalesOrder::getChannelOrderNo, keyword)
                    .or().like(SalesOrder::getReceiverName, keyword).or().like(SalesOrder::getReceiverPhone, keyword)
                    .or().like(SalesOrder::getTrackingNo, keyword));
        }
        qw.eq(StringUtils.isNotBlank(status), SalesOrder::getStatus, status)
                .eq(StringUtils.isNotBlank(channelCode), SalesOrder::getChannelCode, channelCode)
                .eq(StringUtils.isNotBlank(shopCode), SalesOrder::getShopCode, shopCode)
                .eq(StringUtils.isNotBlank(warehouseCode), SalesOrder::getWarehouseCode, warehouseCode)
                .eq(StringUtils.isNotBlank(source), SalesOrder::getSource, source)
                .ge(from != null, SalesOrder::getOrderTime, from == null ? null : from.atStartOfDay())
                .lt(to != null, SalesOrder::getOrderTime, to == null ? null : to.plusDays(1).atStartOfDay())
                .orderByDesc(SalesOrder::getPriority).orderByDesc(SalesOrder::getId);
        return R.ok(orderMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/status-count")
    public R<Map<String, Long>> statusCount() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String s : Arrays.asList(OrderService.CREATED, OrderService.HOLD, OrderService.AUDITED, OrderService.ALLOCATED,
                OrderService.PUSHED, OrderService.SHIPPED, OrderService.COMPLETED, OrderService.CANCELLED, OrderService.SPLIT)) {
            m.put(s, orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getStatus, s)));
        }
        return R.ok(m);
    }

    @Data
    public static class OrderDetail {
        private SalesOrder order;
        private List<SalesOrderItem> items;
        private List<OrderLog> logs;
        private List<SalesOrder> children;
    }

    @GetMapping("/{orderNo}")
    public R<OrderDetail> detail(@PathVariable String orderNo) {
        OrderDetail d = new OrderDetail();
        d.setOrder(orderService.get(orderNo));
        d.setItems(orderService.items(orderNo));
        d.setLogs(orderService.logs(orderNo));
        d.setChildren(orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getParentOrderNo, orderNo)));
        return R.ok(d);
    }

    @PostMapping
    public R<SalesOrder> create(@RequestBody OrderCreateRequest req) {
        return R.ok(orderService.create(req));
    }

    @Data
    public static class ReasonRequest {
        private String reason;
        private String remark;
        private Integer priority;
    }

    @PostMapping("/{orderNo}/audit")
    public R<SalesOrder> audit(@PathVariable String orderNo, @RequestBody(required = false) ReasonRequest req) {
        return R.ok(orderService.audit(orderNo, req == null ? "人工审核" : req.getRemark()));
    }

    @PostMapping("/{orderNo}/hold")
    public R<SalesOrder> hold(@PathVariable String orderNo, @RequestBody ReasonRequest req) {
        return R.ok(orderService.hold(orderNo, req.getReason()));
    }

    @PostMapping("/{orderNo}/unhold")
    public R<SalesOrder> unhold(@PathVariable String orderNo) {
        return R.ok(orderService.unhold(orderNo));
    }

    @PostMapping("/{orderNo}/allocate")
    public R<List<SalesOrder>> allocate(@PathVariable String orderNo) {
        return R.ok(orderService.allocate(orderNo));
    }

    @PostMapping("/{orderNo}/push")
    public R<SalesOrder> push(@PathVariable String orderNo) {
        return R.ok(orderService.push(orderNo));
    }

    @PostMapping("/{orderNo}/auto")
    public R<List<SalesOrder>> auto(@PathVariable String orderNo) {
        return R.ok(orderService.autoProcess(orderNo));
    }

    @Data
    public static class ShipRequest {
        private String carrierCode;
        private String trackingNo;
        private String wmsOrderNo;
    }

    /** 人工登记发货（无 WMS 对接时） */
    @PostMapping("/{orderNo}/ship")
    public R<SalesOrder> ship(@PathVariable String orderNo, @RequestBody ShipRequest req) {
        if (StringUtils.isBlank(req.getTrackingNo())) {
            throw new BizException("运单号必填");
        }
        return R.ok(orderService.shipped(orderNo, req.getCarrierCode(), req.getTrackingNo(), req.getWmsOrderNo(), null));
    }

    @PostMapping("/{orderNo}/complete")
    public R<SalesOrder> complete(@PathVariable String orderNo, @RequestBody(required = false) ReasonRequest req) {
        return R.ok(orderService.signed(orderNo, req == null ? "人工确认完成" : req.getRemark()));
    }

    @PostMapping("/{orderNo}/cancel")
    public R<SalesOrder> cancel(@PathVariable String orderNo, @RequestBody ReasonRequest req) {
        if (StringUtils.isBlank(req.getReason())) {
            throw new BizException("取消原因必填");
        }
        return R.ok(orderService.cancel(orderNo, req.getReason()));
    }

    @PostMapping("/{orderNo}/remark")
    public R<SalesOrder> remark(@PathVariable String orderNo, @RequestBody ReasonRequest req) {
        return R.ok(orderService.updateRemark(orderNo, req.getRemark(), req.getPriority()));
    }

    @Data
    public static class SplitRequest {
        /** itemId -> qty */
        private Map<Long, Integer> items;
    }

    @PostMapping("/{orderNo}/split")
    public R<SalesOrder> split(@PathVariable String orderNo, @RequestBody SplitRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException("请选择需要拆分的明细");
        }
        return R.ok(orderService.split(orderNo, req.getItems()));
    }

    @Data
    public static class BatchRequest {
        private List<String> orderNos;
        private String action;
        private String reason;
    }

    /** 批量操作：audit / allocate / push / auto / cancel，逐单执行并返回失败明细 */
    @PostMapping("/batch")
    public R<Map<String, Object>> batch(@RequestBody BatchRequest req) {
        List<String> ok = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();
        for (String no : req.getOrderNos()) {
            try {
                switch (req.getAction()) {
                    case "audit": orderService.audit(no, "批量审核"); break;
                    case "allocate": orderService.allocate(no); break;
                    case "push": orderService.push(no); break;
                    case "auto": orderService.autoProcess(no); break;
                    case "cancel": orderService.cancel(no, StringUtils.isBlank(req.getReason()) ? "批量取消" : req.getReason()); break;
                    default: throw new BizException("不支持的操作 " + req.getAction());
                }
                ok.add(no);
            } catch (RuntimeException e) {
                failed.put(no, e.getMessage());
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("success", ok);
        m.put("failed", failed);
        return R.ok(m);
    }

    public static final String[] IMPORT_HEADERS = {"shopCode", "channelOrderNo", "receiverName", "receiverPhone", "province", "city",
            "district", "address", "sku", "qty", "price", "buyerRemark"};

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() {
        List<Object[]> sample = Collections.singletonList(new Object[]{"SHOP01", "TB2025010100001", "张三", "13800000000", "广东省", "深圳市", "南山区",
                "科技园 1 号", "SKU001", 2, 99.00, "尽快发货"});
        return Csv.download("order_import_template.csv", IMPORT_HEADERS, sample, r -> r);
    }

    /** CSV 导入：同一 channelOrderNo 的多行合并为一单 */
    @PostMapping("/import")
    public R<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        List<String[]> rows = Csv.read(file.getInputStream());
        if (rows.size() < 2) {
            throw new BizException("文件为空");
        }
        Map<String, Integer> idx = new HashMap<>();
        String[] header = rows.get(0);
        for (int i = 0; i < header.length; i++) {
            idx.put(header[i].trim(), i);
        }
        for (String h : new String[]{"shopCode", "channelOrderNo", "receiverName", "receiverPhone", "address", "sku", "qty"}) {
            if (!idx.containsKey(h)) {
                throw new BizException("缺少列: " + h);
            }
        }
        LinkedHashMap<String, OrderCreateRequest> grouped = new LinkedHashMap<>();
        for (int r = 1; r < rows.size(); r++) {
            String[] c = rows.get(r);
            String key = cell(c, idx, "shopCode") + "|" + cell(c, idx, "channelOrderNo");
            OrderCreateRequest req = grouped.computeIfAbsent(key, k -> {
                OrderCreateRequest q = new OrderCreateRequest();
                q.setSource("IMPORT");
                q.setShopCode(cell(c, idx, "shopCode"));
                q.setChannelOrderNo(cell(c, idx, "channelOrderNo"));
                q.setReceiverName(cell(c, idx, "receiverName"));
                q.setReceiverPhone(cell(c, idx, "receiverPhone"));
                q.setProvince(cell(c, idx, "province"));
                q.setCity(cell(c, idx, "city"));
                q.setDistrict(cell(c, idx, "district"));
                q.setAddress(cell(c, idx, "address"));
                q.setBuyerRemark(cell(c, idx, "buyerRemark"));
                q.setItems(new ArrayList<>());
                return q;
            });
            OrderCreateRequest.Item it = new OrderCreateRequest.Item();
            it.setSku(cell(c, idx, "sku"));
            String qty = cell(c, idx, "qty");
            it.setQty(StringUtils.isBlank(qty) ? 1 : Integer.parseInt(qty));
            String price = cell(c, idx, "price");
            it.setPrice(StringUtils.isBlank(price) ? null : new BigDecimal(price));
            req.getItems().add(it);
        }
        List<String> created = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();
        for (Map.Entry<String, OrderCreateRequest> e : grouped.entrySet()) {
            try {
                created.add(orderService.create(e.getValue()).getOrderNo());
            } catch (RuntimeException ex) {
                failed.put(e.getKey(), ex.getMessage());
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("created", created);
        m.put("failed", failed);
        return R.ok(m);
    }

    private static String cell(String[] c, Map<String, Integer> idx, String name) {
        Integer i = idx.get(name);
        return i == null || i >= c.length ? null : c[i];
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<SalesOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), SalesOrder::getStatus, status).orderByDesc(SalesOrder::getId).last("LIMIT 5000");
        String[] headers = {"订单号", "渠道", "店铺", "渠道单号", "状态", "收件人", "电话", "地址", "仓库", "承运商", "运单号", "应付金额", "下单时间"};
        return Csv.download("orders.csv", headers, orderMapper.selectList(qw), o -> new Object[]{o.getOrderNo(), o.getChannelCode(),
                o.getShopCode(), o.getChannelOrderNo(), o.getStatus(), o.getReceiverName(), o.getReceiverPhone(),
                String.join("", nvl(o.getProvince()), nvl(o.getCity()), nvl(o.getDistrict()), o.getAddress()),
                o.getWarehouseCode(), o.getCarrierCode(), o.getTrackingNo(), o.getPayAmount(), o.getOrderTime()});
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
