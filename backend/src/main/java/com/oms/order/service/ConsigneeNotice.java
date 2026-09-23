package com.oms.order.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 发运后通知收货人。有手机号走短信，客户有邮箱再走邮件。两者都没有则不发。 */
public final class ConsigneeNotice {
    private ConsigneeNotice() {}

    public static final class Draft {
        private final String channel;
        private final String target;
        private final String content;

        public Draft(String channel, String target, String content) {
            this.channel = channel;
            this.target = target;
            this.content = content;
        }

        public String getChannel() {
            return channel;
        }

        public String getTarget() {
            return target;
        }

        public String getContent() {
            return content;
        }
    }

    public static List<Draft> plan(String phone, String email, String orderNo, String trackingNo) {
        String text = "订单 " + (orderNo == null ? "" : orderNo) + " 已发运，运单号 " + (trackingNo == null ? "" : trackingNo);
        List<Draft> drafts = new ArrayList<Draft>();
        if (phone != null && !phone.trim().isEmpty()) {
            drafts.add(new Draft("SMS", phone.trim(), text));
        }
        if (email != null && email.contains("@")) {
            drafts.add(new Draft("EMAIL", email.trim(), text));
        }
        return Collections.unmodifiableList(drafts);
    }
}
