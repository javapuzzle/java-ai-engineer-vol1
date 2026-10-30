package com.acme.retail.ch17.observability;

import com.acme.retail.ch17.assistant.AiResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiCallLoggerTest {

    @Test
    void recordsLatencyAndTokenMetricsOnSuccess() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiCallLogger logger = new AiCallLogger(new AiCallMetrics(registry));

        AiResponse response = logger.call("Is this in stock?",
                prompt -> new AiResponse("Yes, it is in stock.", "acme-assistant-v1", 5, 6));

        assertThat(response.content()).isEqualTo("Yes, it is in stock.");
        assertThat(registry.get("acme.ai.call.latency").tag("outcome", "success").timer().count())
                .isEqualTo(1);
        assertThat(registry.get("acme.ai.call.tokens").tag("type", "prompt").counter().count())
                .isEqualTo(5.0);
        assertThat(registry.get("acme.ai.call.tokens").tag("type", "completion").counter().count())
                .isEqualTo(6.0);
    }

    @Test
    void recordsAnErrorOutcomeWhenTheAiCallFails() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiCallLogger logger = new AiCallLogger(new AiCallMetrics(registry));

        assertThrows(RuntimeException.class, () -> logger.call("Where is my order?", prompt -> {
            throw new RuntimeException("AI provider timeout");
        }));

        assertThat(registry.get("acme.ai.call.latency").tag("outcome", "error").timer().count())
                .isEqualTo(1);
    }
}
