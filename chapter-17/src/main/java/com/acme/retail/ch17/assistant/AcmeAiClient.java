package com.acme.retail.ch17.assistant;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for the real call to Acme Retail's AI provider (see Chapter 10).
 *
 * <p>This chapter is not about making the AI more correct -- that is
 * Chapter 11's problem (grounding and RAG). It is about making sure that,
 * whatever the AI says, Sarah can find out afterwards exactly what it said,
 * how long it took, and how many tokens it used. To keep that story visible,
 * this client still reproduces the Chapter 11 style of confidently wrong
 * answer for "is this in stock" questions.
 */
@Component
public class AcmeAiClient {

    private static final String MODEL = "acme-assistant-v1";

    public AiResponse ask(String prompt) {
        simulateNetworkLatency();

        String content = buildReply(prompt);
        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(content);

        return new AiResponse(content, MODEL, promptTokens, completionTokens);
    }

    private String buildReply(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("stock") || lower.contains("available")) {
            // Confidently answers from general knowledge, not from Acme's
            // live inventory table. Chapter 11 fixes this with grounding.
            // Chapter 17 just makes sure we can prove it happened.
            return "Yes, that item is available in our store.";
        }
        if (lower.contains("return") || lower.contains("refund")) {
            return "You can return this item within 30 days of delivery.";
        }
        return "Thanks for your question. Let me look into that for you.";
    }

    private int estimateTokens(String text) {
        int wordCount = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
        return Math.max(1, wordCount + ThreadLocalRandom.current().nextInt(0, 4));
    }

    private void simulateNetworkLatency() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(150, 650));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
