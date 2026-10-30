package com.acme.retail.ch10.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acme Retail's AI shopping assistant.
 *
 * <p>This is the improved version of the endpoint Sarah wired up for Maya.
 * Her first attempt just forwarded the customer's message straight to the
 * model and returned whatever text came back. It "worked" -- it answered
 * questions -- but Alex's question, "What exactly should the AI know?",
 * exposed two gaps that this version fixes:
 *
 * <ol>
 *   <li>The model had no system prompt, so it had no idea it was supposed
 *       to be Acme Retail's assistant, or which categories Acme Retail even
 *       sells. It was answering from general knowledge, not from Acme
 *       Retail's catalog.</li>
 *   <li>The response was raw text, so the frontend had nothing structured
 *       to render -- no separate product name, category, or reason fields.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/shopping-assistant")
public class ShoppingAssistantController {

    /**
     * The system message: instructions the model receives before it ever
     * sees the customer's message. It sets the assistant's role and hands
     * it the only catalog context it is allowed to rely on. The {@code %s}
     * placeholder is filled in with {@link CatalogContextProvider#summary()}
     * at request time.
     */
    private static final String SYSTEM_PROMPT = """
            You are Acme Retail's shopping assistant. You help customers find
            products from Acme Retail's catalog and explain, briefly, why a
            recommendation fits what they asked for.

            Here is what you currently know about Acme Retail's catalog:
            %s

            Rules:
            - Only recommend products from the categories listed above.
            - If the customer's request does not match any category above,
              say so instead of inventing a product.
            - Keep the "reason" to one or two short sentences.
            """;

    private final ChatClient chatClient;

    private final CatalogContextProvider catalogContext;

    public ShoppingAssistantController(ChatClient shoppingAssistantChatClient,
                                        CatalogContextProvider catalogContext) {
        this.chatClient = shoppingAssistantChatClient;
        this.catalogContext = catalogContext;
    }

    @PostMapping("/recommend")
    public ProductRecommendation recommend(@RequestBody ShoppingAssistantRequest request) {
        String systemPrompt = SYSTEM_PROMPT.formatted(catalogContext.summary());

        return chatClient.prompt()
                .system(systemPrompt)
                .user(request.message())
                .call()
                .entity(ProductRecommendation.class);
    }
}
