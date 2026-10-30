package com.acme.retail.ch13.chat;

import java.time.Instant;

/**
 * One turn of a conversation.
 *
 * <p>Only USER and ASSISTANT turns are stored in {@link ChatMemoryStore}.
 * The system prompt (the assistant's instructions) is <em>not</em> part of
 * the stored history -- it is added fresh, in code, on every single call.
 * Storing it would waste space and would make it harder to change the
 * assistant's instructions without touching every saved session.
 */
public record ChatMessage(Role role, String content, Instant timestamp) {

    public enum Role {
        USER,
        ASSISTANT
    }
}
