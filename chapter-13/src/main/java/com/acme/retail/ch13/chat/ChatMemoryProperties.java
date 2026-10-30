package com.acme.retail.ch13.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for how much chat history the application keeps.
 *
 * <p>Bound from {@code chat.memory.*} in {@code application.yml}. These two
 * numbers are the entire "memory budget" of the assistant: how many turns
 * ride along in every prompt, and how long an idle session survives before
 * Redis reclaims it.
 */
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

    /**
     * Maximum number of chat turns (user and assistant messages combined)
     * kept per session. Older turns are dropped once this limit is
     * exceeded. Every turn kept here is resent, in full, on every future
     * call for that session -- so this number is a direct lever on token
     * cost and latency, not just memory usage.
     */
    private int maxTurns = 12;

    /**
     * How long an idle session's history survives in Redis before it
     * expires. Reset on every new turn, so an active conversation never
     * expires mid-chat.
     */
    private Duration ttl = Duration.ofMinutes(30);

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
