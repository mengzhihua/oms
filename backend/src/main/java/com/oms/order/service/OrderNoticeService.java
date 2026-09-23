package com.oms.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.basic.entity.Customer;
import com.oms.basic.mapper.CustomerMapper;
import com.oms.order.entity.OrderNotice;
import com.oms.order.entity.SalesOrder;
import com.oms.order.mapper.OrderNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 发运通知。默认只登记。http 模式把报文发到配置地址，失败不回滚发运。 */
@Service
@RequiredArgsConstructor
public class OrderNoticeService {
    private final OrderNoticeMapper noticeMapper;
    private final CustomerMapper customerMapper;
    private RestTemplate rest = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${oms.notice.mode:mock}")
    private String mode;
    @Value("${oms.notice.base-url:}")
    private String baseUrl;
    @Value("${oms.notice.api-key:}")
    private String apiKey;

    @Transactional
    public List<OrderNotice> record(SalesOrder order) {
        List<OrderNotice> saved = new ArrayList<OrderNotice>();
        if (order == null) {
            return saved;
        }
        for (ConsigneeNotice.Draft draft : ConsigneeNotice.plan(order.getReceiverPhone(), emailOf(order.getCustomerCode()), order.getOrderNo(), order.getTrackingNo())) {
            if (exists(order.getOrderNo(), draft.getChannel(), draft.getTarget())) {
                continue;
            }
            OrderNotice row = new OrderNotice();
            row.setOrderNo(order.getOrderNo());
            row.setChannel(draft.getChannel());
            row.setTarget(draft.getTarget());
            row.setContent(draft.getContent());
            row.setStatus(submit(row));
            noticeMapper.insert(row);
            saved.add(row);
        }
        return saved;
    }

    public List<OrderNotice> list(String orderNo) {
        return noticeMapper.selectList(new LambdaQueryWrapper<OrderNotice>()
                .eq(OrderNotice::getOrderNo, orderNo)
                .orderByAsc(OrderNotice::getId));
    }

    public static String remark(List<OrderNotice> rows) {
        StringBuilder text = new StringBuilder();
        if (rows == null) {
            return "";
        }
        for (OrderNotice row : rows) {
            if (text.length() > 0) {
                text.append("；");
            }
            text.append("SMS".equals(row.getChannel()) ? "短信" : "邮件")
                    .append(" ")
                    .append(row.getTarget())
                    .append(" ")
                    .append(label(row.getStatus()));
        }
        return text.toString();
    }

    private boolean exists(String orderNo, String channel, String target) {
        Long count = noticeMapper.selectCount(new LambdaQueryWrapper<OrderNotice>()
                .eq(OrderNotice::getOrderNo, orderNo)
                .eq(OrderNotice::getChannel, channel)
                .eq(OrderNotice::getTarget, target));
        return count != null && count > 0;
    }

    private String emailOf(String customerCode) {
        if (customerCode == null || customerCode.trim().isEmpty()) {
            return null;
        }
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>().eq(Customer::getCode, customerCode.trim()));
        return customer == null ? null : customer.getEmail();
    }

    private String submit(OrderNotice row) {
        if (!"http".equalsIgnoreCase(mode) || baseUrl == null || baseUrl.trim().isEmpty()) {
            row.setDetail("已登记，未连接短信或邮件网关");
            return "RECORDED";
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                headers.set("X-Api-Key", apiKey.trim());
            }
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("channel", row.getChannel());
            body.put("to", row.getTarget());
            body.put("text", row.getContent());
            body.put("orderNo", row.getOrderNo());
            body.put("sandbox", Boolean.TRUE);
            String response = rest.postForObject(endpoint(baseUrl), new HttpEntity<Object>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            if (root.path("code").asInt(-1) != 0) {
                row.setDetail(clip(root.path("msg").asText("通知被拒绝")));
                return "FAILED";
            }
            row.setDetail("已提交到通知地址");
            return "SENT";
        } catch (Exception e) {
            row.setDetail(clip(e.getMessage()));
            return "FAILED";
        }
    }

    static String endpoint(String base) {
        return base.trim().replaceAll("/$", "") + "/api/open/notice";
    }

    private static String label(String status) {
        if ("SENT".equals(status)) {
            return "已提交";
        }
        if ("FAILED".equals(status)) {
            return "未送达";
        }
        return "已登记";
    }

    private static String clip(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200);
    }
}
