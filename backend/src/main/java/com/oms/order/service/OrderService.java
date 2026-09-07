package com.oms.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.basic.entity.BundleItem;
import com.oms.basic.entity.Product;
import com.oms.basic.entity.Shop;
import com.oms.basic.mapper.BundleItemMapper;
import com.oms.basic.mapper.ProductMapper;
import com.oms.basic.mapper.ShopMapper;
import com.oms.common.BizException;
import com.oms.common.CodeGenerator;
import com.oms.integration.service.IntegrationService;
import com.oms.inventory.service.InventoryService;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.OrderLog;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import com.oms.order.mapper.OrderLogMapper;
import com.oms.order.mapper.SalesOrderItemMapper;
import com.oms.order.mapper.SalesOrderMapper;
import com.oms.system.auth.CurrentUser;
import com.oms.system.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 销售订单状态机：
 * CREATED -> AUDITED -> ALLOCATED(分仓+预占) -> PUSHED(已推 WMS) -> SHIPPED(WMS 回传发货) -> COMPLETED(签收)
 * CREATED/AUDITED <-> HOLD(挂起)
 * CREATED/AUDITED/HOLD/ALLOCATED/PUSHED -> CANCELLED（ALLOCATED/PUSHED 取消时释放预占，PUSHED 需通知 WMS）
 * ALLOCATED 路由到多仓时父单变为 SPLIT，生成子单
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    public static final String CREATED = "CREATED";
    public static final String HOLD = "HOLD";
    public static final String AUDITED = "AUDITED";
    public static final String ALLOCATED = "ALLOCATED";
    public static final String PUSHED = "PUSHED";
    public static final String SHIPPED = "SHIPPED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String SPLIT = "SPLIT";

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper itemMapper;
    private final OrderLogMapper logMapper;
    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final BundleItemMapper bundleItemMapper;
    private final CodeGenerator codeGenerator;
    private final InventoryService inventoryService;
    private final RoutingService routingService;
    private final IntegrationService integrationService;

    // ---------------------------------------------------------------- 查询

    public SalesOrder get(String orderNo) {
        SalesOrder o = orderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getOrderNo, orderNo));
        if (o == null) {
            throw new BizException("订单不存在: " + orderNo);
        }
        return o;
    }

    public List<SalesOrderItem> items(String orderNo) {
        return itemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderNo, orderNo).orderByAsc(SalesOrderItem::getId));
    }

    public List<OrderLog> logs(String orderNo) {
        return logMapper.selectList(new LambdaQueryWrapper<OrderLog>().eq(OrderLog::getOrderNo, orderNo).orderByAsc(OrderLog::getId));
    }

    public SalesOrder findByChannelOrderNo(String shopCode, String channelOrderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getShopCode, shopCode).eq(SalesOrder::getChannelOrderNo, channelOrderNo).last("LIMIT 1"));
    }

    // ---------------------------------------------------------------- 创建

    @Transactional
    public SalesOrder create(OrderCreateRequest req) {
        if (isBlank(req.getShopCode())) {
            throw new BizException("店铺必填");
        }
        if (isBlank(req.getReceiverName()) || isBlank(req.getReceiverPhone()) || isBlank(req.getAddress())) {
            throw new BizException("收件人、电话、地址必填");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException("订单明细不能为空");
        }
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getCode, req.getShopCode()));
        if (shop == null || shop.getStatus() == null || shop.getStatus() != 1) {
            throw new BizException("店铺不存在或已停用: " + req.getShopCode());
        }
        if (!isBlank(req.getChannelOrderNo()) && findByChannelOrderNo(shop.getCode(), req.getChannelOrderNo()) != null) {
            throw new BizException("渠道订单号已存在: " + req.getChannelOrderNo());
        }

        SalesOrder o = new SalesOrder();
        o.setOrderNo(codeGenerator.next("SO"));
        o.setChannelCode(shop.getChannelCode());
        o.setShopCode(shop.getCode());
        o.setChannelOrderNo(req.getChannelOrderNo());
        o.setSource(isBlank(req.getSource()) ? "MANUAL" : req.getSource());
        o.setCustomerCode(req.getCustomerCode());
        o.setReceiverName(req.getReceiverName());
        o.setReceiverPhone(req.getReceiverPhone());
        o.setProvince(req.getProvince());
        o.setCity(req.getCity());
        o.setDistrict(req.getDistrict());
        o.setAddress(req.getAddress());
        o.setStatus(CREATED);
        o.setPayStatus(isBlank(req.getPayStatus()) ? "PAID" : req.getPayStatus());
        o.setPriority(req.getPriority() == null ? 0 : req.getPriority());
        o.setOrderTime(req.getOrderTime() == null ? LocalDateTime.now() : req.getOrderTime());
        o.setPayTime(req.getPayTime());
        o.setFreight(nz(req.getFreight()));
        o.setDiscount(nz(req.getDiscount()));
        o.setBuyerRemark(req.getBuyerRemark());
        o.setSellerRemark(req.getSellerRemark());

        List<SalesOrderItem> items = expandItems(o.getOrderNo(), req.getItems());
        BigDecimal goods = items.stream().map(SalesOrderItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        o.setGoodsAmount(goods);
        o.setPayAmount(req.getPayAmount() != null ? req.getPayAmount() : goods.add(o.getFreight()).subtract(o.getDiscount()));
        orderMapper.insert(o);
        items.forEach(itemMapper::insert);
        log(o, "CREATE", null, CREATED, "来源 " + o.getSource());

        if (shop.getAutoAudit() != null && shop.getAutoAudit() == 1 && "PAID".equals(o.getPayStatus())) {
            audit(o.getOrderNo(), "店铺自动审核");
            o = get(o.getOrderNo());
        }
        return o;
    }

    /** 组合商品展开为成品明细（保留 bundleSku 以便追溯） */
    private List<SalesOrderItem> expandItems(String orderNo, List<OrderCreateRequest.Item> reqItems) {
        List<SalesOrderItem> out = new ArrayList<>();
        for (OrderCreateRequest.Item ri : reqItems) {
            if (isBlank(ri.getSku()) || ri.getQty() == null || ri.getQty() <= 0) {
                throw new BizException("明细 SKU 与数量必填且数量 > 0");
            }
            Product p = productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getSku, ri.getSku()));
            if (p == null || p.getStatus() == null || p.getStatus() != 1) {
                throw new BizException("商品不存在或已停用: " + ri.getSku());
            }
            BigDecimal price = ri.getPrice() != null ? ri.getPrice() : nz(p.getPrice());
            if ("BUNDLE".equals(p.getType())) {
                List<BundleItem> parts = bundleItemMapper.selectList(new LambdaQueryWrapper<BundleItem>().eq(BundleItem::getBundleSku, p.getSku()));
                if (parts.isEmpty()) {
                    throw new BizException("组合商品未配置明细: " + p.getSku());
                }
                BigDecimal total = price.multiply(BigDecimal.valueOf(ri.getQty()));
                int totalUnits = parts.stream().mapToInt(x -> x.getQty() * ri.getQty()).sum();
                for (BundleItem part : parts) {
                    Product cp = productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getSku, part.getSku()));
                    SalesOrderItem it = new SalesOrderItem();
                    it.setOrderNo(orderNo);
                    it.setSku(part.getSku());
                    it.setProductName(cp != null ? cp.getName() : part.getSku());
                    it.setBundleSku(p.getSku());
                    it.setQty(part.getQty() * ri.getQty());
                    // 组合价按数量均摊到成品
                    it.setAmount(totalUnits == 0 ? BigDecimal.ZERO
                            : total.multiply(BigDecimal.valueOf(it.getQty())).divide(BigDecimal.valueOf(totalUnits), 2, BigDecimal.ROUND_HALF_UP));
                    it.setPrice(it.getAmount().divide(BigDecimal.valueOf(it.getQty()), 2, BigDecimal.ROUND_HALF_UP));
                    it.setReservedQty(0);
                    it.setShippedQty(0);
                    out.add(it);
                }
            } else {
                SalesOrderItem it = new SalesOrderItem();
                it.setOrderNo(orderNo);
                it.setSku(p.getSku());
                it.setProductName(p.getName());
                it.setQty(ri.getQty());
                it.setPrice(price);
                it.setAmount(price.multiply(BigDecimal.valueOf(ri.getQty())));
                it.setReservedQty(0);
                it.setShippedQty(0);
                out.add(it);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 状态流转

    @Transactional
    public SalesOrder audit(String orderNo, String remark) {
        SalesOrder o = get(orderNo);
        require(o, CREATED);
        if (!"PAID".equals(o.getPayStatus())) {
            throw new BizException("订单未支付，不能审核");
        }
        o.setAuditedBy(operator());
        o.setAuditedAt(LocalDateTime.now());
        transit(o, "AUDIT", AUDITED, remark);
        return o;
    }

    @Transactional
    public SalesOrder hold(String orderNo, String reason) {
        SalesOrder o = get(orderNo);
        require(o, CREATED, AUDITED);
        o.setHoldReason(reason);
        transit(o, "HOLD", HOLD, reason);
        return o;
    }

    @Transactional
    public SalesOrder unhold(String orderNo) {
        SalesOrder o = get(orderNo);
        require(o, HOLD);
        o.setHoldReason(null);
        transit(o, "UNHOLD", CREATED, "解除挂起");
        return o;
    }

    /** 分仓路由 + 库存预占；多仓命中时自动拆单 */
    @Transactional
    public List<SalesOrder> allocate(String orderNo) {
        SalesOrder o = get(orderNo);
        require(o, AUDITED);
        List<SalesOrderItem> items = items(orderNo);
        RoutingService.Plan plan = routingService.plan(o, items);
        if (!plan.getShortage().isEmpty()) {
            log(o, "ALLOCATE_FAIL", o.getStatus(), o.getStatus(), "缺货: " + String.join("; ", plan.getShortage()));
            throw new BizException("库存不足，无法分配: " + String.join("; ", plan.getShortage()));
        }
        if (!plan.isSplit()) {
            String wh = plan.getAllocations().keySet().iterator().next();
            reserveAll(o, items, wh);
            o.setWarehouseCode(wh);
            if (o.getCarrierCode() == null) {
                o.setCarrierCode(plan.getCarrierCode());
            }
            transit(o, "ALLOCATE", ALLOCATED, "分配仓库 " + wh);
            return Collections.singletonList(o);
        }
        List<SalesOrder> children = new ArrayList<>();
        int seq = 1;
        for (Map.Entry<String, Map<String, Integer>> e : plan.getAllocations().entrySet()) {
            SalesOrder child = cloneForSplit(o, seq++);
            child.setWarehouseCode(e.getKey());
            child.setCarrierCode(o.getCarrierCode() != null ? o.getCarrierCode() : plan.getCarrierCode());
            child.setStatus(AUDITED);
            List<SalesOrderItem> childItems = new ArrayList<>();
            BigDecimal goods = BigDecimal.ZERO;
            for (Map.Entry<String, Integer> s : e.getValue().entrySet()) {
                int remaining = s.getValue();
                for (SalesOrderItem it : items) {
                    if (!it.getSku().equals(s.getKey()) || remaining <= 0) {
                        continue;
                    }
                    int unshipped = it.getQty() - n(it.getReservedQty());
                    int take = Math.min(remaining, unshipped);
                    if (take <= 0) {
                        continue;
                    }
                    it.setReservedQty(n(it.getReservedQty()) + take); // 临时记录已分配数量，避免重复拆
                    SalesOrderItem ci = copyItem(it, child.getOrderNo(), take);
                    childItems.add(ci);
                    goods = goods.add(ci.getAmount());
                    remaining -= take;
                }
            }
            child.setGoodsAmount(goods);
            child.setPayAmount(goods);
            orderMapper.insert(child);
            childItems.forEach(itemMapper::insert);
            log(child, "SPLIT_CHILD", null, AUDITED, "拆自 " + o.getOrderNo());
            reserveAll(child, childItems, e.getKey());
            transit(child, "ALLOCATE", ALLOCATED, "分配仓库 " + e.getKey());
            children.add(child);
        }
        transit(o, "SPLIT", SPLIT, "拆分为 " + children.size() + " 个子单");
        return children;
    }

    /** 手工拆单：把指定明细数量拆到新子单（子单继承当前状态） */
    @Transactional
    public SalesOrder split(String orderNo, Map<Long, Integer> itemQty) {
        SalesOrder o = get(orderNo);
        require(o, CREATED, AUDITED);
        List<SalesOrderItem> items = items(orderNo);
        SalesOrder child = cloneForSplit(o, (int) (countChildren(orderNo) + 1));
        child.setStatus(o.getStatus());
        List<SalesOrderItem> childItems = new ArrayList<>();
        BigDecimal moved = BigDecimal.ZERO;
        for (SalesOrderItem it : items) {
            Integer q = itemQty.get(it.getId());
            if (q == null || q <= 0) {
                continue;
            }
            if (q > it.getQty()) {
                throw new BizException("拆分数量超过明细数量: " + it.getSku());
            }
            SalesOrderItem ci = copyItem(it, child.getOrderNo(), q);
            childItems.add(ci);
            moved = moved.add(ci.getAmount());
            if (q.equals(it.getQty())) {
                itemMapper.deleteById(it.getId());
            } else {
                it.setQty(it.getQty() - q);
                it.setAmount(it.getPrice().multiply(BigDecimal.valueOf(it.getQty())));
                itemMapper.updateById(it);
            }
        }
        if (childItems.isEmpty()) {
            throw new BizException("请选择需要拆分的明细");
        }
        if (childItems.size() == items.size() && itemMapper.selectCount(new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderNo, orderNo)) == 0) {
            throw new BizException("不能把全部明细拆出");
        }
        child.setGoodsAmount(moved);
        child.setPayAmount(moved);
        child.setParentOrderNo(o.getOrderNo());
        orderMapper.insert(child);
        childItems.forEach(itemMapper::insert);
        o.setGoodsAmount(nz(o.getGoodsAmount()).subtract(moved));
        o.setPayAmount(nz(o.getPayAmount()).subtract(moved));
        orderMapper.updateById(o);
        log(o, "SPLIT", o.getStatus(), o.getStatus(), "手工拆出子单 " + child.getOrderNo());
        log(child, "SPLIT_CHILD", null, child.getStatus(), "拆自 " + o.getOrderNo());
        return child;
    }

    @Transactional
    public SalesOrder push(String orderNo) {
        SalesOrder o = get(orderNo);
        require(o, ALLOCATED);
        List<SalesOrderItem> items = items(orderNo);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", o.getOrderNo());
        payload.put("warehouseCode", o.getWarehouseCode());
        payload.put("carrierCode", o.getCarrierCode());
        payload.put("receiverName", o.getReceiverName());
        payload.put("receiverPhone", o.getReceiverPhone());
        payload.put("address", String.join("", nvl(o.getProvince()), nvl(o.getCity()), nvl(o.getDistrict()), o.getAddress()));
        List<Map<String, Object>> lines = new ArrayList<>();
        for (SalesOrderItem it : items) {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("sku", it.getSku());
            l.put("qty", it.getQty());
            lines.add(l);
        }
        payload.put("items", lines);
        String wmsNo = integrationService.pushOutboundToWms(o.getOrderNo(), payload);
        o.setWmsOrderNo(wmsNo);
        o.setPushedAt(LocalDateTime.now());
        transit(o, "PUSH_WMS", PUSHED, "WMS 单号 " + wmsNo);
        return o;
    }

    /** 一键处理：审核 -> 分配 -> 推送（拆单时对子单继续推送） */
    @Transactional
    public List<SalesOrder> autoProcess(String orderNo) {
        SalesOrder o = get(orderNo);
        if (CREATED.equals(o.getStatus())) {
            audit(orderNo, "一键处理");
        }
        List<SalesOrder> targets = allocate(orderNo);
        List<SalesOrder> out = new ArrayList<>();
        for (SalesOrder t : targets) {
            out.add(push(t.getOrderNo()));
        }
        return out;
    }

    /** WMS 发货回传 */
    @Transactional
    public SalesOrder shipped(String orderNo, String carrierCode, String trackingNo, String wmsOrderNo, Map<String, Integer> shippedQty) {
        SalesOrder o = get(orderNo);
        require(o, PUSHED, ALLOCATED);
        List<SalesOrderItem> items = items(orderNo);
        for (SalesOrderItem it : items) {
            int q = shippedQty == null || shippedQty.isEmpty() ? it.getQty() : shippedQty.getOrDefault(it.getSku(), 0);
            it.setShippedQty(q);
            itemMapper.updateById(it);
            inventoryService.deduct(o.getWarehouseCode(), it.getSku(), n(it.getReservedQty()), o.getOrderNo());
            it.setReservedQty(0);
            itemMapper.updateById(it);
        }
        if (!isBlank(carrierCode)) {
            o.setCarrierCode(carrierCode);
        }
        o.setTrackingNo(trackingNo);
        if (!isBlank(wmsOrderNo)) {
            o.setWmsOrderNo(wmsOrderNo);
        }
        o.setShippedAt(LocalDateTime.now());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", o.getOrderNo());
        payload.put("carrierCode", o.getCarrierCode());
        payload.put("trackingNo", trackingNo);
        payload.put("receiverName", o.getReceiverName());
        payload.put("receiverPhone", o.getReceiverPhone());
        payload.put("address", String.join("", nvl(o.getProvince()), nvl(o.getCity()), nvl(o.getDistrict()), o.getAddress()));
        o.setTmsOrderNo(integrationService.createTransportInTms(o.getOrderNo(), payload));
        transit(o, "SHIP", SHIPPED, "运单 " + trackingNo);
        return o;
    }

    /** TMS 签收回传 -> 完成 */
    @Transactional
    public SalesOrder signed(String orderNo, String remark) {
        SalesOrder o = get(orderNo);
        require(o, SHIPPED);
        o.setSignedAt(LocalDateTime.now());
        o.setCompletedAt(LocalDateTime.now());
        transit(o, "SIGN", COMPLETED, remark == null ? "签收完成" : remark);
        return o;
    }

    @Transactional
    public SalesOrder cancel(String orderNo, String reason) {
        SalesOrder o = get(orderNo);
        require(o, CREATED, AUDITED, HOLD, ALLOCATED, PUSHED);
        if (PUSHED.equals(o.getStatus())) {
            integrationService.cancelOutboundInWms(o.getOrderNo(), o.getWmsOrderNo());
        }
        if (ALLOCATED.equals(o.getStatus()) || PUSHED.equals(o.getStatus())) {
            for (SalesOrderItem it : items(orderNo)) {
                if (n(it.getReservedQty()) > 0) {
                    inventoryService.release(o.getWarehouseCode(), it.getSku(), it.getReservedQty(), o.getOrderNo());
                    it.setReservedQty(0);
                    itemMapper.updateById(it);
                }
            }
        }
        o.setCancelReason(reason);
        transit(o, "CANCEL", CANCELLED, reason);
        return o;
    }

    @Transactional
    public SalesOrder updateRemark(String orderNo, String sellerRemark, Integer priority) {
        SalesOrder o = get(orderNo);
        o.setSellerRemark(sellerRemark);
        if (priority != null) {
            o.setPriority(priority);
        }
        orderMapper.updateById(o);
        log(o, "REMARK", o.getStatus(), o.getStatus(), sellerRemark);
        return o;
    }

    public void log(SalesOrder o, String action, String from, String to, String remark) {
        OrderLog l = new OrderLog();
        l.setOrderNo(o.getOrderNo());
        l.setAction(action);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setOperator(operator());
        l.setRemark(remark);
        logMapper.insert(l);
    }

    // ---------------------------------------------------------------- helpers

    private void reserveAll(SalesOrder o, List<SalesOrderItem> items, String warehouseCode) {
        for (SalesOrderItem it : items) {
            inventoryService.reserve(warehouseCode, it.getSku(), it.getQty(), o.getOrderNo());
            it.setReservedQty(it.getQty());
            itemMapper.updateById(it);
        }
    }

    private SalesOrder cloneForSplit(SalesOrder o, int seq) {
        SalesOrder c = new SalesOrder();
        c.setOrderNo(o.getOrderNo() + "-" + seq);
        c.setParentOrderNo(o.getOrderNo());
        c.setChannelCode(o.getChannelCode());
        c.setShopCode(o.getShopCode());
        c.setChannelOrderNo(o.getChannelOrderNo());
        c.setSource(o.getSource());
        c.setCustomerCode(o.getCustomerCode());
        c.setReceiverName(o.getReceiverName());
        c.setReceiverPhone(o.getReceiverPhone());
        c.setProvince(o.getProvince());
        c.setCity(o.getCity());
        c.setDistrict(o.getDistrict());
        c.setAddress(o.getAddress());
        c.setPayStatus(o.getPayStatus());
        c.setPriority(o.getPriority());
        c.setOrderTime(o.getOrderTime());
        c.setPayTime(o.getPayTime());
        c.setFreight(BigDecimal.ZERO);
        c.setDiscount(BigDecimal.ZERO);
        c.setBuyerRemark(o.getBuyerRemark());
        c.setSellerRemark(o.getSellerRemark());
        c.setAuditedBy(o.getAuditedBy());
        c.setAuditedAt(o.getAuditedAt());
        c.setCarrierCode(o.getCarrierCode());
        return c;
    }

    private SalesOrderItem copyItem(SalesOrderItem it, String orderNo, int qty) {
        SalesOrderItem ci = new SalesOrderItem();
        ci.setOrderNo(orderNo);
        ci.setSku(it.getSku());
        ci.setProductName(it.getProductName());
        ci.setBundleSku(it.getBundleSku());
        ci.setQty(qty);
        ci.setPrice(it.getPrice());
        ci.setAmount(nz(it.getPrice()).multiply(BigDecimal.valueOf(qty)));
        ci.setReservedQty(0);
        ci.setShippedQty(0);
        return ci;
    }

    private long countChildren(String orderNo) {
        return orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getParentOrderNo, orderNo));
    }

    private void transit(SalesOrder o, String action, String to, String remark) {
        String from = o.getStatus();
        o.setStatus(to);
        orderMapper.updateById(o);
        log(o, action, from, to, remark);
    }

    private static void require(SalesOrder o, String... allowed) {
        for (String s : allowed) {
            if (s.equals(o.getStatus())) {
                return;
            }
        }
        throw new BizException("订单 " + o.getOrderNo() + " 当前状态 " + o.getStatus() + " 不允许此操作");
    }

    private static String operator() {
        User u = CurrentUser.get();
        return u != null ? u.getUsername() : "system";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int n(Integer v) {
        return v == null ? 0 : v;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
