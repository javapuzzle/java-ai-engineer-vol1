package com.acme.retail.ch19.assistant;

import com.acme.retail.ch19.product.Product;
import com.acme.retail.ch19.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A deterministic integration test.
 *
 * <p>Testcontainers starts a real, disposable PostgreSQL database in
 * Docker, so this test exercises the same repository, service, and
 * controller wiring that runs in production -- no in-memory database
 * standing in for the real thing. The {@link AssistantAiClient} bean is
 * replaced with a stub that always returns the exact same string.
 *
 * <p>Because BOTH the database contents and the AI client's output are
 * fixed, this test can safely assert an exact match on the HTTP response,
 * the same way any other Spring MVC integration test would. It proves the
 * retrieval-and-prompt-assembly code path is wired correctly. It proves
 * NOTHING about whether a real model would answer well -- that question
 * belongs to {@code ShoppingAssistantEvaluationTest} in the {@code eval}
 * package, not to this test.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShoppingAssistantIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /**
     * This is the mocked AI provider. It replaces {@link OpenAiAssistantClient}
     * with a fixed function, so the test never makes a network call and
     * never sees a different answer on a re-run.
     */
    @TestConfiguration
    static class StubAiClientConfig {

        @Bean
        @Primary
        AssistantAiClient stubAssistantAiClient() {
            return (systemPrompt, question) ->
                    "Based on our inventory, the Trail Runner 42 is sold out (0 units in stock).";
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void seedOneKnownProduct() {
        productRepository.deleteAll();
        productRepository.save(new Product(
                "Trail Runner 42", "Black trail running shoe",
                8999, "SHOE-BLK-42", 0, "Footwear"));
    }

    @Test
    void answerIsExactlyReproducibleBecauseTheAiClientIsStubbed() {
        AskQuestionRequest request = new AskQuestionRequest("Is the black trail running shoe in stock?");

        ResponseEntity<AssistantAnswer> response = restTemplate.postForEntity(
                "/assistant/products/SHOE-BLK-42/ask", request, AssistantAnswer.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        AssistantAnswer body = response.getBody();
        assertThat(body).isNotNull();

        // An exact-match assertion on AI output is only safe here because
        // the AI client is a deterministic stub, not a real model.
        assertThat(body.answer())
                .isEqualTo("Based on our inventory, the Trail Runner 42 is sold out (0 units in stock).");
        assertThat(body.groundingContext())
                .contains("SHOE-BLK-42")
                .contains("sold out, 0 units in stock");
    }
}
