package com.acme.retail.ch16.assistant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The customer-facing endpoint for the Acme Retail shopping assistant.
 *
 * <p>Notice what is missing compared to chapter 07: no {@code try/catch}
 * around the AI call, no {@code if (aiIsDown)} check. The controller
 * stays simple on purpose. All resilience lives in
 * {@link ShoppingAssistantService}, wired declaratively with annotations,
 * so this controller never needs to know the AI provider can fail.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final ShoppingAssistantService shoppingAssistantService;

    public AssistantController(ShoppingAssistantService shoppingAssistantService) {
        this.shoppingAssistantService = shoppingAssistantService;
    }

    @PostMapping("/ask")
    public AssistantAnswer ask(@RequestBody AssistantRequest request) {
        return shoppingAssistantService.askAssistant(request.question());
    }
}
