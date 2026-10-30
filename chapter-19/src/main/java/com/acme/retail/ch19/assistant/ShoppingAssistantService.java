package com.acme.retail.ch19.assistant;

import com.acme.retail.ch19.inventory.InventoryService;
import org.springframework.stereotype.Service;

/**
 * Builds a grounded prompt from real inventory facts, then asks the AI
 * client to answer the customer's question from those facts alone.
 *
 * <p>This class does not know or care whether {@link AssistantAiClient} is
 * the real {@link OpenAiAssistantClient} or a test stub. That is the whole
 * point: the same production code path runs under the deterministic
 * integration test and under the golden-dataset evaluation test.
 */
@Service
public class ShoppingAssistantService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are the Acme Retail shopping assistant.
            Answer the customer's question using ONLY the inventory facts
            below. If the facts do not answer the question, say you are
            not sure -- never guess a stock level or price.

            Inventory facts:
            %s
            """;

    private final InventoryService inventoryService;
    private final AssistantAiClient aiClient;

    public ShoppingAssistantService(InventoryService inventoryService, AssistantAiClient aiClient) {
        this.inventoryService = inventoryService;
        this.aiClient = aiClient;
    }

    public AssistantAnswer answer(String sku, String customerQuestion) {
        String groundingFacts = inventoryService.groundingFactsFor(sku);
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(groundingFacts);
        String answerText = aiClient.ask(systemPrompt, customerQuestion);
        return new AssistantAnswer(sku, customerQuestion, answerText, groundingFacts);
    }
}
