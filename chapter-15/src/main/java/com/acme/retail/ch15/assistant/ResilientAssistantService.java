package com.acme.retail.ch15.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Tries the primary provider first. If that call fails for any reason --
 * an outage, a timeout, a rate limit -- it falls back to the secondary
 * provider automatically, at runtime, with no deploy required.
 *
 * <p>This is the piece Chapter 09's architecture made possible but never
 * actually exercised: two independently swappable providers, proven to
 * both work, with an explicit policy for what happens when one fails.
 */
@Service
public class ResilientAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ResilientAssistantService.class);

    private final ChatClient primaryChatClient;
    private final ChatClient fallbackChatClient;

    public ResilientAssistantService(@Qualifier("primaryChatClient") ChatClient primaryChatClient,
                                      @Qualifier("fallbackChatClient") ChatClient fallbackChatClient) {
        this.primaryChatClient = primaryChatClient;
        this.fallbackChatClient = fallbackChatClient;
    }

    public AssistantAnswer ask(String question) {
        try {
            String answer = primaryChatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            return new AssistantAnswer(answer, ProviderUsed.PRIMARY);
        } catch (RuntimeException primaryFailure) {
            log.warn("Primary provider failed, falling back to secondary provider", primaryFailure);

            String answer = fallbackChatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            return new AssistantAnswer(answer, ProviderUsed.FALLBACK);
        }
    }
}
