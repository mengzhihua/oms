package com.oms.basic.controller;

import com.oms.basic.entity.Carrier;
import com.oms.basic.mapper.CarrierMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/carrier")
public class CarrierController extends BaseCrudController<Carrier, CarrierMapper> {
    public CarrierController() {
        super(Carrier.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
