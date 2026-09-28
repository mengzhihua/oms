package com.oms.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oms.integration.entity.IntegrationLog;
import com.oms.integration.mapper.IntegrationLogMapper;
import com.oms.integration.service.IntegrationService;
import com.oms.order.entity.SalesOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** DMS 回推只在订单事务提交后发送;非本机 http 地址默认拒绝。 */
@SpringBootTest
@ActiveProfiles("test")
class DmsNotifyTest {
    @Autowired IntegrationService integrationService;
    @Autowired IntegrationLogMapper logMapper;
    @Autowired TransactionTemplate tx;

    private static SalesOrder dmsOrder() {
        SalesOrder o = new SalesOrder();
        o.setOrderNo("SO-T" + UUID.randomUUID().toString().substring(0, 8));
        o.setChannelCode("DMS");
        o.setShopCode("SHOP-DMS01");
        o.setChannelOrderNo("RPL-T" + o.getOrderNo());
        o.setStatus("SHIPPED");
        return o;
    }

    private long notifyCount(String orderNo) {
        return logMapper.selectCount(new LambdaQueryWrapper<IntegrationLog>()
                .eq(IntegrationLog::getTarget, IntegrationService.DMS)
                .eq(IntegrationLog::getRefNo, orderNo));
    }

    @Test
    void notifyDeferredUntilCommitAndSkippedOnRollback() {
        SalesOrder rolledBack = dmsOrder();
        tx.executeWithoutResult(s -> {
            integrationService.notifyDms(rolledBack, Collections.emptyList(), "SHIPPED");
            assertEquals(0, notifyCount(rolledBack.getOrderNo()), "事务内不应已发送");
            s.setRollbackOnly();
        });
        assertEquals(0, notifyCount(rolledBack.getOrderNo()), "回滚后不应回推");

        SalesOrder committed = dmsOrder();
        tx.executeWithoutResult(s -> {
            integrationService.notifyDms(committed, Collections.emptyList(), "SHIPPED");
            assertEquals(0, notifyCount(committed.getOrderNo()), "事务内不应已发送");
        });
        assertEquals(1, notifyCount(committed.getOrderNo()), "提交后应回推一次");

        SalesOrder noTx = dmsOrder();
        integrationService.notifyDms(noTx, Collections.emptyList(), "CANCELLED");
        assertEquals(1, notifyCount(noTx.getOrderNo()), "无事务时立即发送");
    }

    @Test
    void logFailureAfterCommitDoesNotPropagate() {
        SalesOrder o = dmsOrder();
        o.setOrderNo("SO-" + String.join("", java.util.Collections.nCopies(80, "X"))); // ref_no VARCHAR(64) 溢出 -> 日志写入失败
        assertDoesNotThrow(() -> tx.executeWithoutResult(s ->
                integrationService.notifyDms(o, Collections.emptyList(), "SHIPPED")));
        assertDoesNotThrow(() -> integrationService.notifyDms(o, Collections.emptyList(), "SIGNED"));
    }

    @Test
    void plainHttpToRemoteHostRejectedByDefault() {
        assertThrows(IllegalStateException.class, () -> integrationService.checkUrl("http://dms.example.com"));
        assertDoesNotThrow(() -> integrationService.checkUrl("https://dms.example.com"));
        assertDoesNotThrow(() -> integrationService.checkUrl("http://localhost:8080"));
        assertDoesNotThrow(() -> integrationService.checkUrl("http://127.0.0.1:8080"));
        assertDoesNotThrow(() -> integrationService.checkUrl(""));
    }
}
