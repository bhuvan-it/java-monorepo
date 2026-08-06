package com.acme.orderservice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.common.core.Result;
import com.acme.domain.model.Money;
import com.acme.domain.model.Order;
import com.acme.domain.model.OrderLine;
import com.acme.orderservice.service.OrderAppService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderAppService orderAppService;

    @Test
    void testCreateOrderSuccess200() throws Exception {
        OrderLine line = new OrderLine("SKU-1", 1, Money.of("15.00", "USD"));
        Order order = Order.create("CUST-100", List.of(line)).orElseThrow();

        given(orderAppService.createOrder("CUST-100", "SKU-1", 1, "15.00", "USD"))
                .willReturn(Result.ok(order));

        mockMvc.perform(post("/orders")
                        .param("customerId", "CUST-100")
                        .param("sku", "SKU-1")
                        .param("quantity", "1")
                        .param("amount", "15.00")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("CUST-100"));
    }

    @Test
    void testCreateOrderBadRequest400() throws Exception {
        given(orderAppService.createOrder(eq(""), anyString(), anyInt(), anyString(), anyString()))
                .willReturn(Result.err("customerId must not be blank"));

        mockMvc.perform(post("/orders")
                        .param("customerId", "")
                        .param("sku", "SKU-1")
                        .param("quantity", "1")
                        .param("amount", "15.00")
                        .param("currency", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("customerId must not be blank"));
    }
}
