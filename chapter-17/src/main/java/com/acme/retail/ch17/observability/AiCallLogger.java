package com.acme.retail.ch17.observability;

import com.acme.retail.ch17.assistant.AiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Wraps every call to the AI assistant with structured logging and metrics.
 *
 * <p>This is the piece that answers "how do we know what happened?" It does
 * three things around every AI call:
 *
 * <ol>
 *   <li>Times it.</li>
 *   <li>Logs the prompt, the response, the latency and the token usage as
 *       structured fields (via MDC), not as a free-text sentence.</li>
 *   <li>Records latency and token metrics in Micrometer.</li>
 * </ol>
 *
 * <p>Because {@link com.acme.retail.ch17.correlation.CorrelationIdFilter}
 * already put a correlation ID into the MDC before this code ever runs, the
 * log line this class writes automatically carries that same correlation
 * ID -- no extra plumbing required.
 */
@Component
public class AiCallLogger {

    private static final Logger log = LoggerFactory.getLogger(AiCallLogger.class);

    private final AiCallMetrics metrics;

    public AiCallLogger(AiCallMetrics metrics) {
        this.metrics = metrics;
    }

    public AiResponse call(String prompt, Function<String, AiResponse> aiCall) {
        Instant start = Instant.now();
        try {
            AiResponse response = aiCall.apply(prompt);
            Duration latency = Duration.between(start, Instant.now());

            logSuccess(prompt, response, latency);
            metrics.recordLatency("success", latency);
            metrics.recordTokens("prompt", response.promptTokens());
            metrics.recordTokens("completion", response.completionTokens());

            return response;
        } catch (RuntimeException ex) {
            Duration latency = Duration.between(start, Instant.now());
            logFailure(prompt, ex, latency);
            metrics.recordLatency("error", latency);
            throw ex;
        }
    }

    private void logSuccess(String prompt, AiResponse response, Duration latency) {
        MDC.put("aiModel", response.model());
        MDC.put("aiLatencyMs", String.valueOf(latency.toMillis()));
        MDC.put("aiPromptTokens", String.valueOf(response.promptTokens()));
        MDC.put("aiCompletionTokens", String.valueOf(response.completionTokens()));
        try {
            log.info("ai_call_completed prompt=\"{}\" response=\"{}\"",
                    sanitize(prompt), sanitize(response.content()));
        } finally {
            MDC.remove("aiModel");
            MDC.remove("aiLatencyMs");
            MDC.remove("aiPromptTokens");
            MDC.remove("aiCompletionTokens");
        }
    }

    private void logFailure(String prompt, Exception ex, Duration latency) {
        MDC.put("aiLatencyMs", String.valueOf(latency.toMillis()));
        try {
            log.error("ai_call_failed prompt=\"{}\" error=\"{}\"",
                    sanitize(prompt), ex.getMessage());
        } finally {
            MDC.remove("aiLatencyMs");
        }
    }

    /** Keeps quotes and newlines from breaking the log line or the JSON encoder. */
    private String sanitize(String text) {
        return text == null ? "" : text.replace("\"", "'").replace("\n", " ");
    }
}
