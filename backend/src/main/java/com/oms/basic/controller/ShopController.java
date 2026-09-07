package com.oms.basic.controller;

import com.oms.basic.entity.Shop;
import com.oms.basic.mapper.ShopMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/shop")
public class ShopController extends BaseCrudController<Shop, ShopMapper> {
    public ShopController() {
        super(Shop.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
