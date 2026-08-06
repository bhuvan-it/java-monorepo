package com.acme.orderservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.domain.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCreateOrderRealHttpRoundTrip() {
        ResponseEntity<Order> response = restTemplate.postForEntity(
                "/orders?customerId=CUST-IT&sku=SKU-IT&quantity=1&amount=10.00&currency=USD",
                null,
                Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().customerId()).isEqualTo("CUST-IT");
    }
}
