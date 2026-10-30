package com.acme.retail.ch17.assistant;

import com.acme.retail.ch17.correlation.CorrelationIdFilter;
import com.acme.retail.ch17.observability.AiCallLogger;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The endpoint the storefront calls to talk to the shopping assistant.
 *
 * <p>Notice what this controller does NOT do: it does not log the prompt or
 * response itself, and it does not touch Micrometer directly. That
 * responsibility belongs to {@link AiCallLogger}, so every AI call in the
 * application -- from any controller, not just this one -- is observed the
 * same way.
 */
@RestController
@RequestMapping("/api/assistant")
public class ChatController {

    private final AcmeAiClient aiClient;

    private final AiCallLogger aiCallLogger;

    public ChatController(AcmeAiClient aiClient, AiCallLogger aiCallLogger) {
        this.aiClient = aiClient;
        this.aiCallLogger = aiCallLogger;
    }

    @PostMapping("/messages")
    public ChatResponse handleMessage(@RequestBody ChatRequest request) {
        AiResponse aiResponse = aiCallLogger.call(request.message(), aiClient::ask);
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return new ChatResponse(correlationId, aiResponse.content());
    }
}
