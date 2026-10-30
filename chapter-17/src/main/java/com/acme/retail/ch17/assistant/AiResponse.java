package com.acme.retail.ch17.assistant;

/**
 * What the AI actually returned for one call: the content, which model
 * produced it, and how many tokens it used. Tokens are the small chunks of
 * text a language model reads and writes (roughly a word or part of a word);
 * providers bill and rate-limit by token count, so tracking them is part of
 * observability, not just cost reporting.
 */
public record AiResponse(String content, String model, int promptTokens, int completionTokens) {

    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}
