package com.oms.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.aftersale.dto.ReturnCreateRequest;
import com.oms.aftersale.entity.Refund;
import com.oms.aftersale.entity.ReturnItem;
import com.oms.aftersale.entity.ReturnOrder;
import com.oms.aftersale.mapper.RefundMapper;
import com.oms.aftersale.mapper.ReturnItemMapper;
import com.oms.aftersale.mapper.ReturnOrderMapper;
import com.oms.common.BizException;
import com.oms.common.CodeGenerator;
import com.oms.integration.service.IntegrationService;
import com.oms.inventory.service.InventoryService;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import com.oms.order.service.OrderService;
import com.oms.system.auth.CurrentUser;
import com.oms.system.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 售后单：type = RETURN(退货退款) / REFUND_ONLY(仅退款) / EXCHANGE(换货)
 * 状态：CREATED -> AUDITED -> [RECEIVED(退货入库)] -> [REFUNDED] -> COMPLETED；任意未完成态可 REJECTED / CANCELLED
 * - RETURN：审核后推 WMS 退货入库单；收货后库存回增，再退款完成
 * - REFUND_ONLY：审核后直接退款完成
 * - EXCHANGE：收货后自动创建 0 元换货销售订单并完成
 */
@Service
@RequiredArgsConstructor
public class AfterSaleService {
    public static final String RETURN = "RETURN";
    public static final String REFUND_ONLY = "REFUND_ONLY";
    public static final String EXCHANGE = "EXCHANGE";

    public static final String CREATED = "CREATED";
    public static final String AUDITED = "AUDITED";
    public static final String RECEIVED = "RECEIVED";
    public static final String REFUNDED = "REFUNDED";
    public static final String COMPLETED = "COMPLETED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private final ReturnOrderMapper returnMapper;
    private final ReturnItemMapper itemMapper;
    private final RefundMapper refundMapper;
    private final CodeGenerator codeGenerator;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final IntegrationService integrationService;

    public ReturnOrder get(String returnNo) {
        ReturnOrder r = returnMapper.selectOne(new LambdaQueryWrapper<ReturnOrder>().eq(ReturnOrder::getReturnNo, returnNo));
        if (r == null) {
            throw new BizException("售后单不存在: " + returnNo);
        }
        return r;
    }

    public List<ReturnItem> items(String returnNo) {
        return itemMapper.selectList(new LambdaQueryWrapper<ReturnItem>().eq(ReturnItem::getReturnNo, returnNo));
    }

    public List<Refund> refunds(String returnNo) {
        return refundMapper.selectList(new LambdaQueryWrapper<Refund>().eq(Refund::getReturnNo, returnNo));
    }

    @Transactional
    public ReturnOrder create(ReturnCreateRequest req) {
        SalesOrder order = orderService.get(req.getOrderNo());
        String type = req.getType() == null ? RETURN : req.getType();
        if (!Arrays.asList(RETURN, REFUND_ONLY, EXCHANGE).contains(type)) {
            throw new BizException("不支持的售后类型 " + type);
        }
        if (OrderService.CANCELLED.equals(order.getStatus()) || OrderService.SPLIT.equals(order.getStatus())) {
            throw new BizException("订单状态 " + order.getStatus() + " 不能发起售后");
        }
        if (!REFUND_ONLY.equals(type) && !OrderService.SHIPPED.equals(order.getStatus()) && !OrderService.COMPLETED.equals(order.getStatus())) {
            throw new BizException("订单尚未发货，请直接取消订单或申请仅退款");
        }
        List<SalesOrderItem> orderItems = orderService.items(order.getOrderNo());
        Map<String, SalesOrderItem> bySku = orderItems.stream().collect(Collectors.toMap(SalesOrderItem::getSku, x -> x, (a, b) -> a));

        ReturnOrder r = new ReturnOrder();
        r.setReturnNo(codeGenerator.next("RT"));
        r.setOrderNo(order.getOrderNo());
        r.setType(type);
        r.setStatus(CREATED);
        r.setChannelCode(order.getChannelCode());
        r.setShopCode(order.getShopCode());
        r.setCustomerCode(order.getCustomerCode());
        r.setReason(req.getReason());
        r.setRemark(req.getRemark());
        r.setWarehouseCode(req.getWarehouseCode() != null ? req.getWarehouseCode() : order.getWarehouseCode());
        r.setReturnTrackingNo(req.getReturnTrackingNo());

        List<ReturnItem> items = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        if (req.getItems() != null) {
            for (ReturnCreateRequest.Item ri : req.getItems()) {
                SalesOrderItem oi = bySku.get(ri.getSku());
                if (oi == null) {
                    throw new BizException("订单中不存在 SKU " + ri.getSku());
                }
                if (ri.getQty() == null || ri.getQty() <= 0 || ri.getQty() > oi.getQty()) {
                    throw new BizException("SKU " + ri.getSku() + " 退货数量不合法");
                }
                ReturnItem it = new ReturnItem();
                it.setReturnNo(r.getReturnNo());
                it.setSku(ri.getSku());
                it.setProductName(oi.getProductName());
                it.setQty(ri.getQty());
                it.setReceivedQty(0);
                it.setAmount(nz(oi.getPrice()).multiply(BigDecimal.valueOf(ri.getQty())));
                it.setExchangeSku(ri.getExchangeSku());
                amount = amount.add(it.getAmount());
                items.add(it);
            }
        }
        if (items.isEmpty() && !REFUND_ONLY.equals(type)) {
            throw new BizException("退货/换货必须选择商品明细");
        }
        r.setRefundAmount(EXCHANGE.equals(type) ? BigDecimal.ZERO
                : req.getRefundAmount() != null ? req.getRefundAmount()
                : items.isEmpty() ? nz(order.getPayAmount()) : amount);
        returnMapper.insert(r);
        items.forEach(itemMapper::insert);
        orderService.log(order, "AFTER_SALE", order.getStatus(), order.getStatus(), "创建售后单 " + r.getReturnNo() + " (" + type + ")");
        return r;
    }

    @Transactional
    public ReturnOrder audit(String returnNo) {
        ReturnOrder r = get(returnNo);
        require(r, CREATED);
        r.setAuditedBy(operator());
        r.setAuditedAt(LocalDateTime.now());
        r.setStatus(AUDITED);
        returnMapper.updateById(r);
        if (REFUND_ONLY.equals(r.getType())) {
            return refund(returnNo, r.getRefundAmount(), "ORIGINAL");
        }
        if (r.getWarehouseCode() != null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("returnNo", r.getReturnNo());
            payload.put("orderNo", r.getOrderNo());
            payload.put("warehouseCode", r.getWarehouseCode());
            payload.put("items", items(returnNo).stream().map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sku", i.getSku());
                m.put("qty", i.getQty());
                return m;
            }).collect(Collectors.toList()));
            integrationService.pushReturnToWms(r.getReturnNo(), payload);
        }
        return r;
    }

    @Transactional
    public ReturnOrder reject(String returnNo, String reason) {
        ReturnOrder r = get(returnNo);
        require(r, CREATED, AUDITED);
        r.setRemark(reason);
        r.setStatus(REJECTED);
        returnMapper.updateById(r);
        return r;
    }

    @Transactional
    public ReturnOrder cancel(String returnNo) {
        ReturnOrder r = get(returnNo);
        require(r, CREATED, AUDITED);
        r.setStatus(CANCELLED);
        returnMapper.updateById(r);
        return r;
    }

    /** 退货入库（WMS 回传或人工）：库存回增；换货则创建换货订单 */
    @Transactional
    public ReturnOrder receive(String returnNo, Map<String, Integer> receivedQty, String warehouseCode) {
        ReturnOrder r = get(returnNo);
        require(r, AUDITED);
        if (warehouseCode != null) {
            r.setWarehouseCode(warehouseCode);
        }
        if (r.getWarehouseCode() == null) {
            throw new BizException("退货仓库未指定");
        }
        for (ReturnItem it : items(returnNo)) {
            int q = receivedQty == null || receivedQty.isEmpty() ? it.getQty() : receivedQty.getOrDefault(it.getSku(), 0);
            it.setReceivedQty(q);
            itemMapper.updateById(it);
            if (q > 0) {
                inventoryService.restock(r.getWarehouseCode(), it.getSku(), q, r.getReturnNo());
            }
        }
        r.setReceivedAt(LocalDateTime.now());
        r.setStatus(RECEIVED);
        returnMapper.updateById(r);
        if (EXCHANGE.equals(r.getType())) {
            createExchangeOrder(r);
            complete(r);
        }
        return r;
    }

    @Transactional
    public ReturnOrder refund(String returnNo, BigDecimal amount, String method) {
        ReturnOrder r = get(returnNo);
        require(r, AUDITED, RECEIVED);
        if (RETURN.equals(r.getType()) && !RECEIVED.equals(r.getStatus())) {
            throw new BizException("退货单需先完成退货入库再退款");
        }
        Refund f = new Refund();
        f.setRefundNo(codeGenerator.next("RF"));
        f.setReturnNo(r.getReturnNo());
        f.setOrderNo(r.getOrderNo());
        f.setAmount(amount == null ? nz(r.getRefundAmount()) : amount);
        f.setMethod(method == null ? "ORIGINAL" : method);
        f.setStatus("PAID");
        f.setPaidAt(LocalDateTime.now());
        f.setOperator(operator());
        refundMapper.insert(f);
        r.setRefundAmount(f.getAmount());
        r.setRefundedAt(LocalDateTime.now());
        r.setStatus(REFUNDED);
        returnMapper.updateById(r);
        complete(r);
        return r;
    }

    private void complete(ReturnOrder r) {
        r.setStatus(COMPLETED);
        r.setCompletedAt(LocalDateTime.now());
        returnMapper.updateById(r);
        SalesOrder o = orderService.get(r.getOrderNo());
        orderService.log(o, "AFTER_SALE_DONE", o.getStatus(), o.getStatus(), "售后单 " + r.getReturnNo() + " 完成");
    }

    private void createExchangeOrder(ReturnOrder r) {
        SalesOrder origin = orderService.get(r.getOrderNo());
        OrderCreateRequest req = new OrderCreateRequest();
        req.setShopCode(origin.getShopCode());
        req.setSource("EXCHANGE");
        req.setChannelOrderNo(r.getReturnNo());
        req.setCustomerCode(origin.getCustomerCode());
        req.setReceiverName(origin.getReceiverName());
        req.setReceiverPhone(origin.getReceiverPhone());
        req.setProvince(origin.getProvince());
        req.setCity(origin.getCity());
        req.setDistrict(origin.getDistrict());
        req.setAddress(origin.getAddress());
        req.setPayAmount(BigDecimal.ZERO);
        req.setSellerRemark("换货单，源售后 " + r.getReturnNo());
        List<OrderCreateRequest.Item> items = new ArrayList<>();
        for (ReturnItem it : items(r.getReturnNo())) {
            OrderCreateRequest.Item x = new OrderCreateRequest.Item();
            x.setSku(it.getExchangeSku() == null ? it.getSku() : it.getExchangeSku());
            x.setQty(it.getReceivedQty() == null || it.getReceivedQty() == 0 ? it.getQty() : it.getReceivedQty());
            x.setPrice(BigDecimal.ZERO);
            items.add(x);
        }
        req.setItems(items);
        SalesOrder ex = orderService.create(req);
        r.setExchangeOrderNo(ex.getOrderNo());
        returnMapper.updateById(r);
    }

    private static void require(ReturnOrder r, String... allowed) {
        for (String s : allowed) {
            if (s.equals(r.getStatus())) {
                return;
            }
        }
        throw new BizException("售后单 " + r.getReturnNo() + " 当前状态 " + r.getStatus() + " 不允许此操作");
    }

    private static String operator() {
        User u = CurrentUser.get();
        return u != null ? u.getUsername() : "system";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
