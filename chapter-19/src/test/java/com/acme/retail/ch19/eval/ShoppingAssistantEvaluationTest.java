package com.acme.retail.ch19.eval;

import com.acme.retail.ch19.assistant.AssistantAnswer;
import com.acme.retail.ch19.assistant.ShoppingAssistantService;
import com.acme.retail.ch19.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The golden-dataset evaluation test -- NOT a deterministic unit test.
 *
 * <p>This test calls the REAL {@link com.acme.retail.ch19.assistant.OpenAiAssistantClient},
 * not a stub. A real model is probabilistic: asking the same question
 * twice can return differently worded answers. So this test never asserts
 * an exact string. Instead it scores every answer against the
 * {@link GoldenDataset} using {@link GroundednessScorer}, and only fails
 * when an answer is ungrounded (it contradicts a known inventory fact) or
 * irrelevant (it never actually addresses the product that was asked
 * about).
 *
 * <p>This test is skipped unless {@code OPENAI_API_KEY} is set. That is
 * deliberate, not a shortcut: a real evaluation suite is usually run as its
 * own credentialed pipeline stage -- for example nightly, or before a
 * release -- rather than on every commit from every laptop. Functional
 * correctness (the other test in this module) and AI quality (this test)
 * are checked on different schedules because they answer different
 * questions.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class ShoppingAssistantEvaluationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ShoppingAssistantService assistantService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void seedGoldenProducts() {
        productRepository.deleteAll();
        productRepository.saveAll(GoldenDataset.products());
    }

    @Test
    void realModelAnswersStayGroundedInKnownInventoryFacts() {
        List<EvaluationResult> results = GoldenDataset.questions().stream()
                .map(question -> {
                    AssistantAnswer answer = assistantService.answer(question.productSku(), question.question());
                    return GroundednessScorer.score(question, answer.answer());
                })
                .toList();

        List<EvaluationResult> failures = results.stream()
                .filter(result -> !result.passed())
                .toList();

        assertThat(failures)
                .withFailMessage(() -> "Ungrounded or irrelevant answers found:%n" + failures)
                .isEmpty();
    }
}
