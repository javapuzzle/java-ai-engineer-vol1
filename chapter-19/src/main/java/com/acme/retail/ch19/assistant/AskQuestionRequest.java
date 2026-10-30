package com.acme.retail.ch19.assistant;

/** The request body for {@code POST /assistant/products/{sku}/ask}. */
public record AskQuestionRequest(String question) {
}
