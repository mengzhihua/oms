package com.oms.basic.controller;

import com.oms.basic.entity.BundleItem;
import com.oms.basic.mapper.BundleItemMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/bundle-item")
public class BundleItemController extends BaseCrudController<BundleItem, BundleItemMapper> {
    public BundleItemController() {
        super(BundleItem.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"bundle_sku", "sku"};
    }
}
