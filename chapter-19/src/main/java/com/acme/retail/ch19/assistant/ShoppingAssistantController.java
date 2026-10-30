package com.acme.retail.ch19.assistant;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one HTTP endpoint this chapter's module exposes. It is intentionally
 * thin -- the interesting behavior lives in {@link ShoppingAssistantService},
 * so both kinds of tests in this chapter can exercise it through the same
 * controller.
 */
@RestController
@RequestMapping("/assistant/products")
public class ShoppingAssistantController {

    private final ShoppingAssistantService assistantService;

    public ShoppingAssistantController(ShoppingAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/{sku}/ask")
    public AssistantAnswer ask(@PathVariable String sku, @RequestBody AskQuestionRequest request) {
        return assistantService.answer(sku, request.question());
    }
}
