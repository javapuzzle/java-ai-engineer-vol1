package com.acme.retail.ch17.assistant;

import com.acme.retail.ch17.correlation.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void echoesTheIncomingCorrelationIdAndReturnsAReply() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CorrelationIdFilter.HEADER_NAME, "ticket-4821");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChatRequest> entity =
                new HttpEntity<>(new ChatRequest("cust-1", "Is this in stock?"), headers);

        ResponseEntity<ChatResponse> response =
                restTemplate.postForEntity("/api/assistant/messages", entity, ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME)).isEqualTo("ticket-4821");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().correlationId()).isEqualTo("ticket-4821");
        assertThat(response.getBody().reply()).isNotBlank();
    }

    @Test
    void generatesACorrelationIdWhenTheCallerDidNotSendOne() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChatRequest> entity =
                new HttpEntity<>(new ChatRequest("cust-2", "Can I return this?"), headers);

        ResponseEntity<ChatResponse> response =
                restTemplate.postForEntity("/api/assistant/messages", entity, ChatResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME)).isNotBlank();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().correlationId()).isNotBlank();
    }
}
