package com.oms.order.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsigneeNoticeTest {
    @Test
    void phoneBecomesSmsAndEmailBecomesMail() {
        List<ConsigneeNotice.Draft> drafts = ConsigneeNotice.plan(" 13800000000 ", "a@b.com", "SO-1", "SF1");
        assertEquals(2, drafts.size());
        assertEquals("SMS", drafts.get(0).getChannel());
        assertEquals("13800000000", drafts.get(0).getTarget());
        assertTrue(drafts.get(0).getContent().contains("SO-1"));
        assertTrue(drafts.get(0).getContent().contains("SF1"));
        assertEquals("EMAIL", drafts.get(1).getChannel());
        assertEquals("a@b.com", drafts.get(1).getTarget());
    }

    @Test
    void blankContactsProduceNothing() {
        assertTrue(ConsigneeNotice.plan(" ", "not-an-email", "SO-1", "SF1").isEmpty());
        assertEquals("http://notice.example/api/open/notice", OrderNoticeService.endpoint("http://notice.example/"));
    }
}
