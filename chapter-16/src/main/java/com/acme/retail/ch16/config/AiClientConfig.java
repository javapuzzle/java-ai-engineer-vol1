package com.acme.retail.ch16.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the timeout for calls to the AI provider.
 *
 * <p>Spring AI's OpenAI client sends its (non-streaming) requests through
 * a {@code RestClient}. By default that client has no read timeout at
 * all — if the AI provider accepts the connection but never sends a
 * response, the calling thread waits forever. That is exactly what
 * happened to Sarah: no exception was ever thrown, so retry and the
 * circuit breaker never even got a chance to run. A timeout must exist
 * before anything else in this chapter can work.
 *
 * <p>Two-and-a-half seconds sounds strict for a chat model, but it fits
 * this chapter's lesson: fail fast, then let {@code Retry} and the
 * circuit breaker decide what happens next, instead of letting one slow
 * call hold a thread hostage.
 */
@Configuration
public class AiClientConfig {

    @Bean
    public RestClientCustomizer aiProviderTimeoutCustomizer() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofMillis(2500));

        return restClientBuilder ->
                restClientBuilder.requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
