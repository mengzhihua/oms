package com.oms.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.aftersale.dto.ReturnCreateRequest;
import com.oms.aftersale.entity.ReturnOrder;
import com.oms.aftersale.service.AfterSaleService;
import com.oms.common.BizException;
import com.oms.integration.entity.IntegrationLog;
import com.oms.integration.mapper.IntegrationLogMapper;
import com.oms.inventory.entity.Inventory;
import com.oms.inventory.mapper.InventoryMapper;
import com.oms.inventory.service.InventoryService;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.SalesOrder;
import com.oms.order.entity.SalesOrderItem;
import com.oms.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** 销售订单全链路：创建 -> 审核 -> 分仓预占 -> 推 WMS -> 发货扣减 -> 签收完成；拆单、取消释放、售后退货回增。 */
@SpringBootTest
@ActiveProfiles("test")
class OrderFlowTest {
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired OrderService orderService;
    @Autowired InventoryService inventoryService;
    @Autowired AfterSaleService afterSaleService;
    @Autowired InventoryMapper inventoryMapper;
    @Autowired IntegrationLogMapper integrationLogMapper;

    private static OrderCreateRequest.Item item(String sku, int qty, String price) {
        OrderCreateRequest.Item i = new OrderCreateRequest.Item();
        i.setSku(sku);
        i.setQty(qty);
        i.setPrice(new BigDecimal(price));
        return i;
    }

    private static OrderCreateRequest req(String shop, String province, OrderCreateRequest.Item... items) {
        OrderCreateRequest r = new OrderCreateRequest();
        r.setShopCode(shop);
        r.setChannelOrderNo("T" + UUID.randomUUID().toString().substring(0, 12) + SEQ.incrementAndGet());
        r.setReceiverName("测试");
        r.setReceiverPhone("13800000000");
        r.setProvince(province);
        r.setCity("市");
        r.setAddress("地址1号");
        r.setPayStatus("PAID");
        r.setItems(Arrays.asList(items));
        return r;
    }

    private Inventory inv(String wh, String sku) {
        return inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, wh).eq(Inventory::getSku, sku));
    }

    @Test
    void happyPathSingleWarehouse() {
        // 广东订单 -> 路由规则"华南就近发货" -> WH-GZ / SF
        SalesOrder o = orderService.create(req("SHOP-JD01", "广东省", item("SKU001", 2, "199")));
        assertEquals(OrderService.CREATED, o.getStatus());
        assertEquals("JD", o.getChannelCode());
        assertEquals(new BigDecimal("398.00"), o.getGoodsAmount().setScale(2));

        orderService.audit(o.getOrderNo(), null);
        int before = InventoryService.available(inv("WH-GZ", "SKU001"));
        List<SalesOrder> allocated = orderService.allocate(o.getOrderNo());
        assertEquals(1, allocated.size());
        assertEquals("WH-GZ", allocated.get(0).getWarehouseCode());
        assertEquals("SF", allocated.get(0).getCarrierCode());
        assertEquals(before - 2, InventoryService.available(inv("WH-GZ", "SKU001")));
        assertTrue(inv("WH-GZ", "SKU001").getQtyReserved() >= 2);

        SalesOrder pushed = orderService.push(o.getOrderNo());
        assertEquals(OrderService.PUSHED, pushed.getStatus());
        assertNotNull(pushed.getWmsOrderNo());
        assertEquals(1, integrationLogMapper.selectCount(new LambdaQueryWrapper<IntegrationLog>()
                .eq(IntegrationLog::getRefNo, o.getOrderNo()).eq(IntegrationLog::getAction, "PUSH_OUTBOUND")));

        int onHandBefore = inv("WH-GZ", "SKU001").getQtyOnHand();
        SalesOrder shipped = orderService.shipped(o.getOrderNo(), "SF", "SF123", null, null);
        assertEquals(OrderService.SHIPPED, shipped.getStatus());
        assertNotNull(shipped.getTmsOrderNo());
        assertEquals(onHandBefore - 2, inv("WH-GZ", "SKU001").getQtyOnHand());
        for (SalesOrderItem it : orderService.items(o.getOrderNo())) {
            assertEquals(2, it.getShippedQty());
            assertEquals(0, it.getReservedQty());
        }

        SalesOrder done = orderService.signed(o.getOrderNo(), "签收");
        assertEquals(OrderService.COMPLETED, done.getStatus());
        assertTrue(orderService.logs(o.getOrderNo()).size() >= 5);

        // 售后退货：审核 -> 入库回增 -> 退款 -> 完成
        ReturnCreateRequest rr = new ReturnCreateRequest();
        rr.setOrderNo(o.getOrderNo());
        rr.setType("RETURN");
        rr.setReason("不想要了");
        ReturnCreateRequest.Item ri = new ReturnCreateRequest.Item();
        ri.setSku("SKU001");
        ri.setQty(1);
        rr.setItems(Collections.singletonList(ri));
        ReturnOrder ret = afterSaleService.create(rr);
        assertEquals(new BigDecimal("199.00"), ret.getRefundAmount().setScale(2));
        afterSaleService.audit(ret.getReturnNo());
        int gzBefore = inv("WH-GZ", "SKU001").getQtyOnHand();
        afterSaleService.receive(ret.getReturnNo(), null, "WH-GZ");
        assertEquals(gzBefore + 1, inv("WH-GZ", "SKU001").getQtyOnHand());
        ReturnOrder refunded = afterSaleService.refund(ret.getReturnNo(), null, null);
        assertEquals("COMPLETED", refunded.getStatus());
        assertEquals(1, afterSaleService.refunds(ret.getReturnNo()).size());
    }

    @Test
    void splitAcrossWarehousesWhenNoSingleWarehouseCanFulfil() {
        // 需要 SKU002 (仅 SH/BJ 有) + SKU005 大量 (SH 200, GZ 50)，广东路由优先 GZ，但 GZ 无 SKU002
        int sh5 = InventoryService.available(inv("WH-SH", "SKU005"));
        int gz5 = InventoryService.available(inv("WH-GZ", "SKU005"));
        int need5 = sh5 + Math.min(gz5, 5); // 单仓不够，需拆到两仓
        // SHOP-TM01 开启自动审核，创建后即为 AUDITED
        SalesOrder o = orderService.create(req("SHOP-TM01", "广东省", item("SKU002", 1, "299"), item("SKU005", need5, "39")));
        assertEquals(OrderService.AUDITED, o.getStatus());
        List<SalesOrder> children = orderService.allocate(o.getOrderNo());
        assertTrue(children.size() >= 2, "should split");
        assertEquals(OrderService.SPLIT, orderService.get(o.getOrderNo()).getStatus());
        int totalSku5 = 0;
        for (SalesOrder c : children) {
            assertEquals(OrderService.ALLOCATED, c.getStatus());
            assertEquals(o.getOrderNo(), c.getParentOrderNo());
            for (SalesOrderItem it : orderService.items(c.getOrderNo())) {
                if ("SKU005".equals(it.getSku())) totalSku5 += it.getQty();
                assertEquals(it.getQty(), it.getReservedQty());
            }
        }
        assertEquals(need5, totalSku5);
        // 取消子单释放预占
        SalesOrder c0 = children.get(0);
        int avail = InventoryService.available(inv(c0.getWarehouseCode(), orderService.items(c0.getOrderNo()).get(0).getSku()));
        orderService.cancel(c0.getOrderNo(), "测试取消");
        SalesOrderItem first = orderService.items(c0.getOrderNo()).get(0);
        assertEquals(avail + first.getQty(), InventoryService.available(inv(c0.getWarehouseCode(), first.getSku())));
        assertEquals(0, first.getReservedQty());
    }

    @Test
    void shortageRejectsAllocation() {
        SalesOrder o = orderService.create(req("SHOP-OFF01", "上海市", item("SKU002", 100000, "1")));
        orderService.audit(o.getOrderNo(), null);
        BizException ex = assertThrows(BizException.class, () -> orderService.allocate(o.getOrderNo()));
        assertTrue(ex.getMessage().contains("库存不足"));
        assertEquals(OrderService.AUDITED, orderService.get(o.getOrderNo()).getStatus());
    }

    @Test
    void bundleExplodesToComponentsAndHoldCancelFlow() {
        SalesOrder o = orderService.create(req("SHOP-OFF01", "上海市", item("BUNDLE-01", 2, "329")));
        List<SalesOrderItem> items = orderService.items(o.getOrderNo());
        assertEquals(3, items.size());
        items.forEach(i -> {
            assertEquals("BUNDLE-01", i.getBundleSku());
            assertEquals(2, i.getQty());
        });
        orderService.hold(o.getOrderNo(), "地址待确认");
        assertEquals(OrderService.HOLD, orderService.get(o.getOrderNo()).getStatus());
        assertThrows(BizException.class, () -> orderService.audit(o.getOrderNo(), null));
        orderService.unhold(o.getOrderNo());
        orderService.cancel(o.getOrderNo(), "买家取消");
        assertEquals(OrderService.CANCELLED, orderService.get(o.getOrderNo()).getStatus());
        assertThrows(BizException.class, () -> orderService.audit(o.getOrderNo(), null));
    }

    @Test
    void duplicateChannelOrderRejectedAndUnpaidCannotAudit() {
        OrderCreateRequest r = req("SHOP-TM01", "上海市", item("SKU001", 1, "199"));
        orderService.create(r);
        assertThrows(BizException.class, () -> orderService.create(r));

        OrderCreateRequest unpaid = req("SHOP-TM01", "上海市", item("SKU001", 1, "199"));
        unpaid.setPayStatus("UNPAID");
        SalesOrder o = orderService.create(unpaid);
        assertEquals(OrderService.CREATED, o.getStatus()); // 未支付不自动审核
        assertThrows(BizException.class, () -> orderService.audit(o.getOrderNo(), null));
    }

    @Test
    void autoProcessGoesStraightToPushed() {
        SalesOrder o = orderService.create(req("SHOP-DY01", "北京市", item("SKU004", 3, "29")));
        List<SalesOrder> out = orderService.autoProcess(o.getOrderNo());
        assertEquals(1, out.size());
        assertEquals(OrderService.PUSHED, out.get(0).getStatus());
        assertEquals("WH-SH", out.get(0).getWarehouseCode()); // 抖音店默认仓 SH（无省份规则命中北京）
    }

    @Test
    void channelAvailableRespectsPolicy() {
        // SHOP-JD01 策略：限定 WH-BJ, 100%
        int bj = Math.max(0, InventoryService.available(inv("WH-BJ", "SKU003")) - inv("WH-BJ", "SKU003").getSafetyQty());
        assertEquals(bj, inventoryService.channelAvailable("SHOP-JD01", "SKU003"));
        // SHOP-DY01 策略：全仓 50%
        int total = 0;
        for (Inventory i : inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>().eq(Inventory::getSku, "SKU004"))) {
            total += Math.max(0, InventoryService.available(i) - (i.getSafetyQty() == null ? 0 : i.getSafetyQty()));
        }
        assertEquals(total / 2, inventoryService.channelAvailable("SHOP-DY01", "SKU004"));
    }
}
