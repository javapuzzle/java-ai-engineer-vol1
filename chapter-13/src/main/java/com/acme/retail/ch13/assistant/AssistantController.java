package com.acme.retail.ch13.assistant;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for the shopping assistant.
 *
 * <p>{@code sessionId} is supplied by the caller (the chat widget), not
 * generated here -- the same session id must be reused across the whole
 * conversation for {@code ShoppingAssistantService} to find the right
 * history in Redis.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final ShoppingAssistantService assistantService;

    public AssistantController(ShoppingAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ChatReply sendMessage(@PathVariable String sessionId, @RequestBody ChatRequest request) {
        String reply = assistantService.reply(sessionId, request.message());
        return new ChatReply(sessionId, reply);
    }
}
