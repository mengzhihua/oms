package com.oms.basic.controller;

import com.oms.basic.entity.Channel;
import com.oms.basic.mapper.ChannelMapper;
import com.oms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/channel")
public class ChannelController extends BaseCrudController<Channel, ChannelMapper> {
    public ChannelController() {
        super(Channel.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
