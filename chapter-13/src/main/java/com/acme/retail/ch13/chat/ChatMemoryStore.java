package com.acme.retail.ch13.chat;

import java.util.List;

/**
 * Stores and retrieves conversation turns for a session.
 *
 * <p>This is the abstraction Sarah introduces so the rest of the code never
 * needs to know <em>where</em> chat history lives. Chapter 13 backs it with
 * Redis ({@link RedisChatMemoryStore}), but a test can back it with a plain
 * {@code HashMap} without touching {@code ShoppingAssistantService} at all.
 */
public interface ChatMemoryStore {

    /**
     * Returns this session's stored turns, oldest first. Returns an empty
     * list for a session that has never been seen (or has expired).
     */
    List<ChatMessage> load(String sessionId);

    /**
     * Appends one turn to this session's history. Implementations are
     * expected to enforce their own cap on history length -- callers should
     * not have to know or care how that cap is enforced.
     */
    void append(String sessionId, ChatMessage message);
}
