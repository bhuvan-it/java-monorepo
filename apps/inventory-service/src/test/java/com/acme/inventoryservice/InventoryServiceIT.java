package com.acme.inventoryservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testInventoryRealHttpRoundTrip() {
        ResponseEntity<Void> postResponse = restTemplate.postForEntity(
                "/api/inventory/stock?productId=IT-PROD-1&quantity=100",
                null,
                Void.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Integer> getResponse = restTemplate.getForEntity(
                "/api/inventory/stock/IT-PROD-1",
                Integer.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(100);
    }
}
