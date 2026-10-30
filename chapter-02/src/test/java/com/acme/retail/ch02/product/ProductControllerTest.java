package com.acme.retail.ch02.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Functional tests: does the endpoint return the right product, and the
 * right status code for a missing one? These pass whether or not virtual
 * threads are enabled — correctness is not the problem in this chapter,
 * throughput under load is (see {@link ConcurrentRequestLoadTest}).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsKnownProduct() {
        ResponseEntity<Product> response =
                restTemplate.getForEntity("/api/products/1", Product.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sku()).isEqualTo("SHOE-TRAIL-01");
    }

    @Test
    void returnsNotFoundForUnknownProduct() {
        ResponseEntity<Product> response =
                restTemplate.getForEntity("/api/products/999", Product.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
