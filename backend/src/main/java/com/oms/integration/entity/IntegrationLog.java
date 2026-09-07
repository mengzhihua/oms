package com.oms.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.oms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_integration_log")
public class IntegrationLog extends BaseEntity {
    private String direction;
    private String target;
    private String action;
    private String refNo;
    private String requestBody;
    private String responseBody;
    private Integer success;
    private String errorMsg;
}
