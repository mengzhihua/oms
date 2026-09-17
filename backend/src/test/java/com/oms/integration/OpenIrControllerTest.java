package com.oms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OpenIrControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void snapshotsIncludeStuckOrderThenHold() throws Exception {
        String snapshots = mockMvc.perform(get("/api/open/ir/snapshots")
                        .header("X-Api-Key", "test-open-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.system").value("OMS"))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(snapshots).get("data");
        JsonNode stuck = null;
        for (JsonNode row : data.get("orders")) {
            if ("IR-SO-STUCK".equals(row.path("orderNo").asText())) {
                stuck = row;
                break;
            }
        }
        assertNotNull(stuck, "应包含 IR 卡单种子");
        boolean sku001 = false;
        for (JsonNode row : data.get("inventory")) {
            if ("SKU001".equals(row.path("sku").asText())
                    && "WH-SH".equals(row.path("warehouseCode").asText())) {
                sku001 = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(sku001, "库存快照应含 WH-SH/SKU001");

        String type = "OMS_HOLD";
        if ("HOLD".equals(stuck.path("status").asText())) {
            type = "OMS_UNHOLD";
        }
        mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "test-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"" + type + "\",\"targetKey\":\"IR-SO-STUCK\","
                                + "\"params\":{\"reason\":\"IR 控制塔挂起\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        if ("OMS_UNHOLD".equals(type)) {
            mockMvc.perform(post("/api/open/ir/actions")
                            .header("X-Api-Key", "test-open-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"OMS_HOLD\",\"targetKey\":\"IR-SO-STUCK\","
                                    + "\"params\":{\"reason\":\"IR 控制塔挂起\"}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.status").value("HOLD"));
        } else {
            String held = mockMvc.perform(get("/api/open/ir/snapshots")
                            .header("X-Api-Key", "test-open-key"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode after = null;
            for (JsonNode row : objectMapper.readTree(held).get("data").get("orders")) {
                if ("IR-SO-STUCK".equals(row.path("orderNo").asText())) {
                    after = row;
                    break;
                }
            }
            assertNotNull(after);
            assertEquals("HOLD", after.path("status").asText());
        }
    }
}
