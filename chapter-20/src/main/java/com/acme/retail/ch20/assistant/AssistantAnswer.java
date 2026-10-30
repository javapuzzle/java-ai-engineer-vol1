package com.acme.retail.ch20.assistant;

/**
 * What the model provider said, plus one extra bit that the model itself
 * never sets: whether that answer contradicts Acme Retail's real inventory.
 *
 * <p>{@code mismatched} is computed by {@link AssistantService}, never by
 * {@link ModelProvider}, on purpose -- the provider should not be trusted
 * to grade its own answer.
 *
 * @param text          the natural-language answer shown to the customer
 * @param claimsInStock what the model's answer asserts about availability
 * @param mismatched    true when {@code claimsInStock} disagrees with the
 *                      real {@link ProductFact#inStock()} used to ask the
 *                      question
 * @param providerId    which provider/version produced this answer -- shown
 *                      in structured logs so an incident can be traced back
 *                      to a specific provider deployment
 */
public record AssistantAnswer(String text, boolean claimsInStock, boolean mismatched, String providerId) {

    /**
     * Returns a copy of this answer with {@code mismatched} set. Kept
     * immutable and separate from the constructor so it reads clearly at
     * the call site: the provider produces the answer, the service decides
     * whether it disagrees with reality.
     */
    public AssistantAnswer withMismatch(boolean value) {
        return new AssistantAnswer(text, claimsInStock, value, providerId);
    }
}
