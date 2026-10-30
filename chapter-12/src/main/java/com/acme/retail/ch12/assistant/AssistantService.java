package com.acme.retail.ch12.assistant;

import com.acme.retail.ch12.product.InventoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Sarah's shopping assistant, wired to a Spring AI {@link ChatClient}.
 *
 * <p>Same question, same grounding facts, two very different user
 * experiences:
 *
 * <ul>
 *   <li>{@link #askAndWaitForFullAnswer(String)} -- the traditional
 *       approach. It blocks until the model has finished the entire
 *       answer, then returns one {@code String}.</li>
 *   <li>{@link #askAndStreamAnswer(String)} -- the modern approach. It
 *       returns a {@link Flux} of answer chunks (deltas) as the model
 *       generates them, so a caller can forward each chunk to the
 *       customer's browser the moment it exists.</li>
 * </ul>
 */
@Service
public class AssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the Acme Retail shopping assistant.
            Only state product availability using the facts listed below.
            If a fact is not listed, say you are not sure and offer to check.
            Keep answers short: two or three sentences.

            Facts:
            %s
            """;

    private final ChatClient chatClient;
    private final InventoryService inventoryService;

    public AssistantService(ChatClient chatClient, InventoryService inventoryService) {
        this.chatClient = chatClient;
        this.inventoryService = inventoryService;
    }

    /** Traditional approach: wait for the complete answer, then return it. */
    public String askAndWaitForFullAnswer(String question) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(inventoryService.catalogFacts()))
                .user(question)
                .call()
                .content();
    }

    /** Modern approach: stream the answer as the model generates it. */
    public Flux<String> askAndStreamAnswer(String question) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT.formatted(inventoryService.catalogFacts()))
                .user(question)
                .stream()
                .content();
    }
}
