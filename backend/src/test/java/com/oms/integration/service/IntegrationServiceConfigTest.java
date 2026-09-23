package com.oms.integration.service;

import com.oms.common.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class IntegrationServiceConfigTest {
    @Test
    public void liveCallWithoutUrlIsRejected() {
        assertThrows(BizException.class, () -> IntegrationService.requireConfigured(false, "", "WMS"));
        assertThrows(BizException.class, () -> IntegrationService.requireConfigured(false, null, "TMS"));
    }

    @Test
    public void mockOrConfiguredUrlIsAllowed() {
        IntegrationService.requireConfigured(true, "", "WMS");
        IntegrationService.requireConfigured(false, "http://localhost:8083", "WMS");
    }
}
