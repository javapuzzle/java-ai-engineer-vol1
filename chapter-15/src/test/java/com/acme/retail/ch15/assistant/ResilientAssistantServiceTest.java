package com.acme.retail.ch15.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves the fallback path actually executes when the primary provider
 * fails -- the whole point of this chapter. Chapters 10-13 already showed
 * that a ChatClient-based service can be unit tested with a mock instead
 * of a real model; this test goes one step further and proves the
 * *failover policy* itself, not just a single happy-path call.
 */
class ResilientAssistantServiceTest {

    @Test
    void ask_usesPrimaryProvider_whenPrimarySucceeds() {
        ChatClient primary = mockChatClientReturning("Answer from primary");
        ChatClient fallback = mock(ChatClient.class);

        ResilientAssistantService service = new ResilientAssistantService(primary, fallback);

        AssistantAnswer answer = service.ask("Are trail shoes in stock?");

        assertThat(answer.providerUsed()).isEqualTo(ProviderUsed.PRIMARY);
        assertThat(answer.text()).isEqualTo("Answer from primary");
    }

    @Test
    void ask_fallsBackToSecondaryProvider_whenPrimaryThrows() {
        ChatClient primary = mock(ChatClient.class, invocation -> {
            throw new RuntimeException("Primary provider outage");
        });
        ChatClient fallback = mockChatClientReturning("Answer from fallback");

        ResilientAssistantService service = new ResilientAssistantService(primary, fallback);

        AssistantAnswer answer = service.ask("Are trail shoes in stock?");

        assertThat(answer.providerUsed()).isEqualTo(ProviderUsed.FALLBACK);
        assertThat(answer.text()).isEqualTo("Answer from fallback");
    }

    private static ChatClient mockChatClientReturning(String content) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(content);

        return chatClient;
    }
}
