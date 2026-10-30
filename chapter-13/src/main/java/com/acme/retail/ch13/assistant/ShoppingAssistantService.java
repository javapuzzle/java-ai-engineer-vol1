package com.acme.retail.ch13.assistant;

import com.acme.retail.ch13.chat.ChatMemoryStore;
import com.acme.retail.ch13.chat.ChatMessage;
import com.acme.retail.ch13.chat.ChatMessage.Role;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The Acme Retail shopping assistant.
 *
 * <p>This is where Sarah's fix lives. Before Chapter 13, {@code reply}
 * built a prompt from only the system message and the customer's newest
 * message -- which is exactly why the assistant "forgot" that the customer
 * had already said "black running shoes" by the time they said "only size
 * 8." An LLM (Large Language Model) call is stateless: it only knows what
 * is inside the prompt it receives, nothing more.
 *
 * <p>The fix is not inside the model. It is here: load this session's
 * prior turns from {@link ChatMemoryStore}, and put them back into the
 * prompt, every single time, before calling the {@link ChatClient}.
 */
@Service
public class ShoppingAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the Acme Retail shopping assistant. Help the customer
            find products, sizes, and availability. Stay on the product the
            customer already mentioned earlier in this conversation unless
            they clearly change the subject.
            """;

    private final ChatClient chatClient;
    private final ChatMemoryStore memoryStore;

    public ShoppingAssistantService(ChatModel chatModel, ChatMemoryStore memoryStore) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.memoryStore = memoryStore;
    }

    /**
     * Answers one customer message within an ongoing session.
     *
     * @param sessionId       identifies the conversation (see
     *                        {@code AssistantController} -- typically the
     *                        customer's chat-widget session, not
     *                        necessarily their logged-in customer id).
     * @param customerMessage the newest thing the customer just said.
     * @return the assistant's reply text.
     */
    public String reply(String sessionId, String customerMessage) {
        List<ChatMessage> history = memoryStore.load(sessionId);

        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(SYSTEM_PROMPT));
        for (ChatMessage turn : history) {
            promptMessages.add(toSpringAiMessage(turn));
        }
        promptMessages.add(new UserMessage(customerMessage));

        String reply = chatClient.prompt()
                .messages(promptMessages)
                .call()
                .content();

        memoryStore.append(sessionId, new ChatMessage(Role.USER, customerMessage, Instant.now()));
        memoryStore.append(sessionId, new ChatMessage(Role.ASSISTANT, reply, Instant.now()));

        return reply;
    }

    private Message toSpringAiMessage(ChatMessage turn) {
        return switch (turn.role()) {
            case USER -> new UserMessage(turn.content());
            case ASSISTANT -> new AssistantMessage(turn.content());
        };
    }
}
