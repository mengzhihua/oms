package com.oms.basic.controller;

import com.oms.basic.entity.Product;
import com.oms.basic.mapper.ProductMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/product")
public class ProductController extends BaseCrudController<Product, ProductMapper> {
    public ProductController() {
        super(Product.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"sku", "name", "barcode"};
    }
}
