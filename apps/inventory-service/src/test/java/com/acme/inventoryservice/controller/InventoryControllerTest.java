package com.acme.inventoryservice.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.inventoryservice.service.InventoryAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryAppService inventoryAppService;

    @Test
    void testAddAndGetStockEndpoints() throws Exception {
        given(inventoryAppService.getAvailableStock("PROD-1")).willReturn(10);

        mockMvc.perform(post("/api/inventory/stock")
                        .param("productId", "PROD-1")
                        .param("quantity", "10"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/stock/PROD-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }
}
