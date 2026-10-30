package com.acme.retail.ch16.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the resilience annotations on {@link ShoppingAssistantService}
 * through a full Spring context.
 *
 * <p>These annotations only do anything because Spring wraps
 * {@code ShoppingAssistantService} in an AOP proxy at startup -- a plain
 * unit test that calls {@code new ShoppingAssistantService(...)} would
 * skip the proxy entirely and the retry / circuit breaker / fallback
 * behavior would never run. That is why this is a {@code @SpringBootTest}
 * instead of a plain JUnit test.
 *
 * <p>{@link AiModelClient} is mocked so the tests never make a real
 * network call to an AI provider -- they only need to prove that
 * {@code ShoppingAssistantService} reacts correctly when that call
 * succeeds, fails once, or fails every time.
 */
@SpringBootTest
@ActiveProfiles("test")
class ShoppingAssistantServiceTest {

    @Autowired
    private ShoppingAssistantService shoppingAssistantService;

    @MockBean
    private AiModelClient aiModelClient;

    @BeforeEach
    void resetMock() {
        reset(aiModelClient);
    }

    @Test
    void returnsTheRealAnswerWhenTheAiProviderSucceeds() {
        when(aiModelClient.complete(anyString(), anyString()))
                .thenReturn("Yes, the black running shoes are in stock.");

        AssistantAnswer answer = shoppingAssistantService.askAssistant(
                "Are the black running shoes in stock?");

        assertThat(answer.degraded()).isFalse();
        assertThat(answer.message()).isEqualTo("Yes, the black running shoes are in stock.");
        verify(aiModelClient, times(1)).complete(anyString(), anyString());
    }

    @Test
    void retriesOnATransientFailureAndThenSucceeds() {
        when(aiModelClient.complete(anyString(), anyString()))
                .thenThrow(new RuntimeException("connection reset"))
                .thenReturn("We restock every Tuesday.");

        AssistantAnswer answer = shoppingAssistantService.askAssistant(
                "When do you restock?");

        assertThat(answer.degraded()).isFalse();
        assertThat(answer.message()).isEqualTo("We restock every Tuesday.");
        // One failed attempt, then one successful retry.
        verify(aiModelClient, times(2)).complete(anyString(), anyString());
    }

    @Test
    void returnsTheFallbackMessageWhenTheAiProviderKeepsTimingOut() {
        when(aiModelClient.complete(anyString(), anyString()))
                .thenThrow(new RuntimeException(new TimeoutException("read timed out")));

        AssistantAnswer answer = shoppingAssistantService.askAssistant(
                "Is the blue jacket available in size M?");

        assertThat(answer.degraded()).isTrue();
        assertThat(answer.message()).isEqualTo(AssistantAnswer.FALLBACK_MESSAGE);
        // Retry exhausts all configured attempts before the circuit
        // breaker gives up and calls the fallback.
        verify(aiModelClient, atLeast(3)).complete(anyString(), anyString());
    }
}
