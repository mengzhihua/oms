package com.oms.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.integration.entity.IntegrationLog;
import com.oms.integration.mapper.IntegrationLogMapper;
import com.oms.integration.service.IntegrationService;
import com.oms.order.dto.OrderCreateRequest;
import com.oms.order.entity.SalesOrder;
import com.oms.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** DMS 备件补货渠道：下单 -> 自动处理 -> 发货/签收时向 DMS 回推状态(mock 模式仅记录集成日志)。 */
@SpringBootTest
@ActiveProfiles("test")
class DmsChannelFlowTest {
    @Autowired OrderService orderService;
    @Autowired IntegrationLogMapper integrationLogMapper;

    @Test
    void dmsOrderNotifiesDmsOnShipAndSign() {
        OrderCreateRequest r = new OrderCreateRequest();
        r.setShopCode("SHOP-DMS01");
        r.setChannelOrderNo("RPL" + UUID.randomUUID().toString().substring(0, 12));
        r.setCustomerCode("D001");
        r.setReceiverName("上海申联汽车4S店");
        r.setReceiverPhone("021-58880001");
        r.setProvince("上海市");
        r.setCity("上海市");
        r.setAddress("浦东新区申江路100号");
        OrderCreateRequest.Item i = new OrderCreateRequest.Item();
        i.setSku("P0001");
        i.setQty(10);
        i.setPrice(new BigDecimal("35"));
        r.setItems(Collections.singletonList(i));

        SalesOrder o = orderService.create(r);
        assertEquals("DMS", o.getChannelCode());
        orderService.autoProcess(o.getOrderNo());
        orderService.shipped(o.getOrderNo(), "SF", "SF" + System.nanoTime(), null, null);
        orderService.signed(o.getOrderNo(), null);

        List<String> actions = integrationLogMapper.selectList(new LambdaQueryWrapper<IntegrationLog>()
                        .eq(IntegrationLog::getTarget, IntegrationService.DMS)
                        .eq(IntegrationLog::getRefNo, o.getOrderNo()))
                .stream().map(IntegrationLog::getAction).collect(Collectors.toList());
        assertTrue(actions.contains("NOTIFY_SHIPPED"), actions.toString());
        assertTrue(actions.contains("NOTIFY_SIGNED"), actions.toString());

        // 非 DMS 渠道不回推
        SalesOrder jd = orderService.create(req());
        orderService.autoProcess(jd.getOrderNo());
        orderService.shipped(jd.getOrderNo(), "SF", "SF" + System.nanoTime(), null, null);
        assertEquals(0, integrationLogMapper.selectCount(new LambdaQueryWrapper<IntegrationLog>()
                .eq(IntegrationLog::getTarget, IntegrationService.DMS)
                .eq(IntegrationLog::getRefNo, jd.getOrderNo())));
    }

    private static OrderCreateRequest req() {
        OrderCreateRequest r = new OrderCreateRequest();
        r.setShopCode("SHOP-TM01");
        r.setChannelOrderNo("T" + UUID.randomUUID().toString().substring(0, 12));
        r.setReceiverName("测试");
        r.setReceiverPhone("13800000000");
        r.setProvince("上海市");
        r.setCity("市");
        r.setAddress("地址1号");
        OrderCreateRequest.Item i = new OrderCreateRequest.Item();
        i.setSku("SKU001");
        i.setQty(1);
        i.setPrice(new BigDecimal("199"));
        r.setItems(Collections.singletonList(i));
        return r;
    }
}
