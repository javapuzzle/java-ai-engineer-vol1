package com.acme.retail.ch11.assistant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes both versions of the assistant so they can be compared directly.
 *
 * <p>{@code /assistant/ask-ungrounded} exists only to demonstrate the bug
 * from this chapter's story -- it should not be called from the real
 * storefront. {@code /assistant/ask} is the grounded endpoint Sarah ships.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final ShoppingAssistantService shoppingAssistantService;

    public AssistantController(ShoppingAssistantService shoppingAssistantService) {
        this.shoppingAssistantService = shoppingAssistantService;
    }

    @PostMapping("/ask-ungrounded")
    public AskResponse askUngrounded(@RequestBody AskRequest request) {
        String answer = shoppingAssistantService.askUngrounded(request.question());
        return new AskResponse(answer, false);
    }

    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        String answer = shoppingAssistantService.askGrounded(request.question(), request.productQuery());
        return new AskResponse(answer, true);
    }

    public record AskRequest(String question, String productQuery) {
    }

    public record AskResponse(String answer, boolean grounded) {
    }
}
