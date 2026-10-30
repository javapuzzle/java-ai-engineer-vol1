package com.acme.retail.ch12.assistant;

import com.acme.retail.ch12.assistant.dto.AssistantRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Two endpoints that answer the same question, so the difference in
 * perceived latency is easy to see side by side.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private static final Logger log = LoggerFactory.getLogger(AssistantController.class);

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Traditional approach. Nothing is written to the HTTP response until
     * the model has produced the entire answer -- the customer's browser
     * has nothing to show until this method returns.
     */
    @PostMapping("/ask")
    public String ask(@RequestBody AssistantRequest request) {
        Instant start = Instant.now();
        String answer = assistantService.askAndWaitForFullAnswer(request.question());
        log.info("Synchronous answer ready after {} ms (total latency)",
                Duration.between(start, Instant.now()).toMillis());
        return answer;
    }

    /**
     * Modern approach. Returns a stream of Server-Sent Events (SSE): each
     * {@code text/event-stream} chunk is a small piece of the answer, sent
     * to the browser the moment the model produces it. The browser can
     * render the first words while the model is still generating the rest.
     */
    @PostMapping(path = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@RequestBody AssistantRequest request) {
        Instant start = Instant.now();
        AtomicReference<Instant> firstTokenAt = new AtomicReference<>();

        return assistantService.askAndStreamAnswer(request.question())
                .doOnNext(chunk -> {
                    if (firstTokenAt.compareAndSet(null, Instant.now())) {
                        log.info("First token after {} ms (first-token latency)",
                                Duration.between(start, firstTokenAt.get()).toMillis());
                    }
                })
                .doOnComplete(() -> log.info("Stream complete after {} ms (total latency)",
                        Duration.between(start, Instant.now()).toMillis()));
    }
}
