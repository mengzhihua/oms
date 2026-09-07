package com.oms.basic.controller;

import com.oms.basic.entity.Warehouse;
import com.oms.basic.mapper.WarehouseMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/warehouse")
public class WarehouseController extends BaseCrudController<Warehouse, WarehouseMapper> {
    public WarehouseController() {
        super(Warehouse.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
