package com.acme.retail.ch13.assistant;

import com.acme.retail.ch13.chat.ChatMemoryStore;
import com.acme.retail.ch13.chat.ChatMessage;
import com.acme.retail.ch13.chat.ChatMessage.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the fix from this chapter: {@code ShoppingAssistantService} loads
 * prior turns for the session and puts them back into the prompt before
 * every {@code ChatClient} call, instead of sending only the newest
 * message. {@link ChatModel} is mocked, so this test never makes a real
 * network call and never needs an API key.
 */
class ShoppingAssistantServiceTest {

    @Test
    void injectsPriorTurnsIntoEveryChatModelCall() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatMemoryStore memoryStore = mock(ChatMemoryStore.class);

        when(memoryStore.load("session-1")).thenReturn(List.of(
                new ChatMessage(Role.USER, "I want black running shoes.", Instant.now()),
                new ChatMessage(Role.ASSISTANT, "Sure.", Instant.now())
        ));

        AssistantMessage assistantMessage = new AssistantMessage("Got it -- black running shoes, size 8.");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ShoppingAssistantService service = new ShoppingAssistantService(chatModel, memoryStore);

        String reply = service.reply("session-1", "Only size 8.");

        assertThat(reply).isEqualTo("Got it -- black running shoes, size 8.");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        List<Message> sentMessages = promptCaptor.getValue().getInstructions();

        // system + 2 stored history turns + the new user turn = 4 messages.
        assertThat(sentMessages).hasSize(4);
        assertThat(sentMessages.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(sentMessages.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(sentMessages.get(1).getText()).isEqualTo("I want black running shoes.");
        assertThat(sentMessages.get(2).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        assertThat(sentMessages.get(2).getText()).isEqualTo("Sure.");
        assertThat(sentMessages.get(3).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(sentMessages.get(3).getText()).isEqualTo("Only size 8.");

        verify(memoryStore).append(eq("session-1"),
                argThat(m -> m.role() == Role.USER && m.content().equals("Only size 8.")));
        verify(memoryStore).append(eq("session-1"),
                argThat(m -> m.role() == Role.ASSISTANT && m.content().equals("Got it -- black running shoes, size 8.")));
    }

    @Test
    void firstMessageInASessionSendsOnlySystemAndUserTurns() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatMemoryStore memoryStore = mock(ChatMemoryStore.class);

        when(memoryStore.load("session-new")).thenReturn(List.of());

        AssistantMessage assistantMessage = new AssistantMessage("Sure, what are you looking for?");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ShoppingAssistantService service = new ShoppingAssistantService(chatModel, memoryStore);
        service.reply("session-new", "I want black running shoes.");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        assertThat(promptCaptor.getValue().getInstructions()).hasSize(2);
    }
}
