package com.oms.basic.controller;

import com.oms.basic.entity.RoutingRule;
import com.oms.basic.mapper.RoutingRuleMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/routing-rule")
public class RoutingRuleController extends BaseCrudController<RoutingRule, RoutingRuleMapper> {
    public RoutingRuleController() {
        super(RoutingRule.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"name", "warehouse_code"};
    }
}
