package com.acme.retail.ch19.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * The production implementation of {@link AssistantAiClient}. Sends the
 * system prompt and the customer's question to a real chat model through
 * Spring AI, and returns whatever text the model generates.
 *
 * <p>This class is never used in the deterministic integration test in
 * this chapter -- it is replaced there by a test stub. It IS used by
 * {@code ShoppingAssistantEvaluationTest}, which is exactly why that test
 * needs a scoring harness instead of an exact-match assertion.
 */
@Service
public class OpenAiAssistantClient implements AssistantAiClient {

    private final ChatClient chatClient;

    public OpenAiAssistantClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String ask(String systemPrompt, String customerQuestion) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(customerQuestion)
                .call()
                .content();
    }
}
