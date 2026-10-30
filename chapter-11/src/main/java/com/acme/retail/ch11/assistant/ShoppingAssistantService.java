package com.acme.retail.ch11.assistant;

import com.acme.retail.ch11.inventory.GroundingContextBuilder;
import com.acme.retail.ch11.inventory.InventoryFact;
import com.acme.retail.ch11.inventory.InventoryLookupService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Answers customer questions about products -- the "before" and "after" of
 * this chapter, side by side.
 */
@Service
public class ShoppingAssistantService {

    private final ChatClient chatClient;
    private final InventoryLookupService inventoryLookupService;

    public ShoppingAssistantService(ChatClient.Builder chatClientBuilder,
                                     InventoryLookupService inventoryLookupService) {
        this.chatClient = chatClientBuilder.build();
        this.inventoryLookupService = inventoryLookupService;
    }

    /**
     * This is what Sarah shipped in Chapter 10, and the bug in this
     * chapter's story. The customer's question goes straight to the model
     * with no system message and no inventory data at all. The model has
     * never seen Acme Retail's {@code products} table -- it was trained on
     * a large, general body of text that says nothing about whether one
     * specific store has one specific shoe in stock today. When it answers
     * "Yes," it is not lying on purpose; it is doing exactly what a
     * language model does when it has no grounding: producing the most
     * plausible-sounding continuation of the conversation. That is a
     * hallucination -- a fluent, confident answer with no basis in fact.
     */
    public String askUngrounded(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * The fix: retrieve the real facts first, then generate.
     *
     * <p>This is Retrieval-Augmented Generation (RAG) in its simplest
     * form. RAG means exactly what its name says: retrieve real
     * information relevant to the question, then let the model generate
     * its answer from that retrieved information, instead of from its
     * training data alone.
     *
     * <ol>
     *   <li><b>Retrieve</b> -- {@link InventoryLookupService} queries the
     *       real {@code products} table for rows matching the question.</li>
     *   <li><b>Ground</b> -- {@link GroundingContextBuilder} turns those
     *       rows into plain text, and {@link GroundedPromptFactory} wraps
     *       that text in an instruction telling the model to answer only
     *       from it.</li>
     *   <li><b>Generate</b> -- the model produces an answer, but now every
     *       claim it can make about stock is traceable back to a real
     *       database row.</li>
     * </ol>
     */
    public String askGrounded(String question, String productQuery) {
        List<InventoryFact> facts = inventoryLookupService.findFacts(productQuery);
        String groundingContext = GroundingContextBuilder.toContext(facts);
        String systemPrompt = GroundedPromptFactory.systemPrompt(groundingContext);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }
}
