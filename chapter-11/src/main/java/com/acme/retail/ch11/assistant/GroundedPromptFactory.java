package com.acme.retail.ch11.assistant;

/**
 * Builds the system prompt that turns a plain question-answering model
 * into a grounded one.
 *
 * <p>Kept separate from {@link ShoppingAssistantService} for the same
 * reason as {@code GroundingContextBuilder}: it is a pure function from
 * text to text, so it can be tested without a real model call.
 */
public final class GroundedPromptFactory {

    private GroundedPromptFactory() {
    }

    /**
     * The instruction that turns retrieved facts into grounding: answer
     * only from the facts given, and say so honestly when the facts do not
     * cover the question. Without the last sentence, a model will happily
     * keep guessing even when handed real data -- grounding only works if
     * the model is told the facts are the boundary of what it may claim.
     */
    public static String systemPrompt(String groundingContext) {
        return """
                You are the Acme Retail shopping assistant.

                Answer the customer's question using ONLY the inventory
                facts listed below. Do not use any other knowledge about
                this product, and do not guess.

                If the facts below do not mention the product the customer
                is asking about, say that you are not sure and offer to
                check with a human associate.

                Inventory facts:
                %s
                """.formatted(groundingContext);
    }
}
