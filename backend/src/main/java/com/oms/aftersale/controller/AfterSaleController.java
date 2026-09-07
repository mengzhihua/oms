package com.oms.aftersale.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oms.aftersale.dto.ReturnCreateRequest;
import com.oms.aftersale.entity.Refund;
import com.oms.aftersale.entity.ReturnItem;
import com.oms.aftersale.entity.ReturnOrder;
import com.oms.aftersale.mapper.RefundMapper;
import com.oms.aftersale.mapper.ReturnOrderMapper;
import com.oms.aftersale.service.AfterSaleService;
import com.oms.common.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aftersale")
@RequiredArgsConstructor
public class AfterSaleController {
    private final ReturnOrderMapper returnMapper;
    private final RefundMapper refundMapper;
    private final AfterSaleService service;

    @GetMapping("/page")
    public R<Page<ReturnOrder>> page(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "20") long size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(required = false) String shopCode) {
        LambdaQueryWrapper<ReturnOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> w.like(ReturnOrder::getReturnNo, keyword).or().like(ReturnOrder::getOrderNo, keyword)
                    .or().like(ReturnOrder::getReturnTrackingNo, keyword));
        }
        qw.eq(StringUtils.isNotBlank(status), ReturnOrder::getStatus, status)
                .eq(StringUtils.isNotBlank(type), ReturnOrder::getType, type)
                .eq(StringUtils.isNotBlank(shopCode), ReturnOrder::getShopCode, shopCode)
                .orderByDesc(ReturnOrder::getId);
        return R.ok(returnMapper.selectPage(new Page<>(current, size), qw));
    }

    @Data
    public static class Detail {
        private ReturnOrder returnOrder;
        private List<ReturnItem> items;
        private List<Refund> refunds;
    }

    @GetMapping("/{returnNo}")
    public R<Detail> detail(@PathVariable String returnNo) {
        Detail d = new Detail();
        d.setReturnOrder(service.get(returnNo));
        d.setItems(service.items(returnNo));
        d.setRefunds(service.refunds(returnNo));
        return R.ok(d);
    }

    @PostMapping
    public R<ReturnOrder> create(@RequestBody ReturnCreateRequest req) {
        return R.ok(service.create(req));
    }

    @PostMapping("/{returnNo}/audit")
    public R<ReturnOrder> audit(@PathVariable String returnNo) {
        return R.ok(service.audit(returnNo));
    }

    @Data
    public static class ReasonRequest {
        private String reason;
    }

    @PostMapping("/{returnNo}/reject")
    public R<ReturnOrder> reject(@PathVariable String returnNo, @RequestBody ReasonRequest req) {
        return R.ok(service.reject(returnNo, req.getReason()));
    }

    @PostMapping("/{returnNo}/cancel")
    public R<ReturnOrder> cancel(@PathVariable String returnNo) {
        return R.ok(service.cancel(returnNo));
    }

    @Data
    public static class ReceiveRequest {
        private String warehouseCode;
        /** sku -> 实收数量；为空表示按申请数量全收 */
        private Map<String, Integer> items;
    }

    @PostMapping("/{returnNo}/receive")
    public R<ReturnOrder> receive(@PathVariable String returnNo, @RequestBody(required = false) ReceiveRequest req) {
        return R.ok(service.receive(returnNo, req == null ? null : req.getItems(), req == null ? null : req.getWarehouseCode()));
    }

    @Data
    public static class RefundRequest {
        private BigDecimal amount;
        private String method;
    }

    @PostMapping("/{returnNo}/refund")
    public R<ReturnOrder> refund(@PathVariable String returnNo, @RequestBody(required = false) RefundRequest req) {
        return R.ok(service.refund(returnNo, req == null ? null : req.getAmount(), req == null ? null : req.getMethod()));
    }

    @GetMapping("/refund/page")
    public R<Page<Refund>> refundPage(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "20") long size,
                                      @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Refund> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            qw.and(w -> w.like(Refund::getRefundNo, keyword).or().like(Refund::getOrderNo, keyword).or().like(Refund::getReturnNo, keyword));
        }
        qw.orderByDesc(Refund::getId);
        return R.ok(refundMapper.selectPage(new Page<>(current, size), qw));
    }
}
