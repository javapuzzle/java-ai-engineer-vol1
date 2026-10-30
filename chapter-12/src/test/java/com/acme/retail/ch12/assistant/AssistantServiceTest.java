package com.acme.retail.ch12.assistant;

import com.acme.retail.ch12.product.InventoryService;
import com.acme.retail.ch12.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link AssistantService} against a hand-written fake
 * {@link ChatModel} instead of a real OpenAI account -- no network call,
 * no API key, and (importantly for this chapter) full control over how
 * fast tokens "arrive," so the test can demonstrate first-token latency
 * being smaller than total latency, exactly the point of Chapter 12.
 */
class AssistantServiceTest {

    private static final List<String> TOKENS =
            List.of("The ", "black ", "running ", "shoe ", "has ", "4 ", "in stock.");

    private final AssistantService assistantService = new AssistantService(
            ChatClient.builder(new FakeStreamingChatModel()).build(),
            new InventoryService(new ProductRepository()));

    @Test
    void askAndWaitForFullAnswer_returnsTheCompleteAnswer() {
        String answer = assistantService.askAndWaitForFullAnswer("Is the black running shoe available?");

        assertThat(answer).isEqualTo(String.join("", TOKENS));
    }

    @Test
    void askAndStreamAnswer_emitsOneChunkPerGeneratedToken() {
        Flux<String> stream = assistantService.askAndStreamAnswer("Is the black running shoe available?");

        StepVerifier.create(stream)
                .expectNextCount(TOKENS.size())
                .verifyComplete();
    }

    @Test
    void firstTokenArrivesBeforeTheStreamFinishes() {
        Instant start = Instant.now();
        AtomicReference<Duration> firstTokenLatency = new AtomicReference<>();

        assistantService.askAndStreamAnswer("Is the black running shoe available?")
                .doOnNext(chunk -> firstTokenLatency.compareAndSet(null, Duration.between(start, Instant.now())))
                .blockLast();

        Duration totalLatency = Duration.between(start, Instant.now());

        assertThat(firstTokenLatency.get()).isNotNull();
        assertThat(firstTokenLatency.get()).isLessThan(totalLatency);
    }

    /**
     * A minimal {@link ChatModel} that "generates" a fixed answer one token
     * at a time, with a small delay between tokens -- enough to make
     * first-token latency measurably smaller than total latency in a fast
     * unit test, the same way it is in a real streaming call to an LLM
     * provider.
     */
    private static class FakeStreamingChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            String fullAnswer = String.join("", TOKENS);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(fullAnswer))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.fromIterable(TOKENS)
                    .delayElements(Duration.ofMillis(20))
                    .map(token -> new ChatResponse(List.of(new Generation(new AssistantMessage(token)))));
        }
    }
}
