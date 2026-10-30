package com.acme.retail.ch19.assistant;

/**
 * One answer from the shopping assistant, including the grounding facts it
 * was given -- so a test (or a curious engineer reading logs) can see
 * exactly what the model was and was not told.
 */
public record AssistantAnswer(String sku, String question, String answer, String groundingContext) {
}
