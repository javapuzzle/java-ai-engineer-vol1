package com.acme.retail.ch17.assistant;

/** A message a customer sends to the Acme Retail shopping assistant. */
public record ChatRequest(String customerId, String message) {
}
