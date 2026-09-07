package com.oms.inventory.controller;

import com.oms.common.BaseCrudController;
import com.oms.inventory.entity.ChannelStockPolicy;
import com.oms.inventory.mapper.ChannelStockPolicyMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory/policy")
public class ChannelStockPolicyController extends BaseCrudController<ChannelStockPolicy, ChannelStockPolicyMapper> {
    public ChannelStockPolicyController() {
        super(ChannelStockPolicy.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"shop_code", "sku", "warehouse_code"};
    }
}
