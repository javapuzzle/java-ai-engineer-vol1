package com.acme.retail.ch12.assistant;

import com.acme.retail.ch12.assistant.dto.AssistantRequest;
import com.acme.retail.ch12.product.InventoryService;
import com.acme.retail.ch12.product.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the HTTP contract of the streaming endpoint: a {@code Flux<String>}
 * returned from the controller must become a {@code text/event-stream}
 * response that the servlet container dispatches asynchronously, chunk by
 * chunk, instead of one blocking write.
 *
 * <p>Rather than mocking {@link AssistantService}, this test wires the real
 * service to the same kind of hand-written fake {@link ChatModel} used in
 * {@link AssistantServiceTest}. That keeps this chapter's tests free of a
 * mocking framework entirely -- one less thing for a beginner to learn, and
 * one less thing that can behave differently across JDKs.
 */
@WebMvcTest(AssistantController.class)
@Import(AssistantControllerTest.FakeAssistantConfig.class)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void askStream_respondsWithServerSentEventsContainingEachChunk() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/assistant/ask/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AssistantRequest("Is the black running shoe available?"))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("in stock")));
    }

    @TestConfiguration
    static class FakeAssistantConfig {

        @Bean
        AssistantService assistantService() {
            ChatClient fakeChatClient = ChatClient.builder(new FixedAnswerChatModel()).build();
            return new AssistantService(fakeChatClient, new InventoryService(new ProductRepository()));
        }
    }

    /** Streams a short, fixed answer -- just enough to prove the SSE plumbing works. */
    private static class FixedAnswerChatModel implements ChatModel {

        private static final List<String> TOKENS = List.of("The ", "shoe ", "has ", "4 ", "in stock.");

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(String.join("", TOKENS)))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.fromIterable(TOKENS)
                    .delayElements(Duration.ofMillis(5))
                    .map(token -> new ChatResponse(List.of(new Generation(new AssistantMessage(token)))));
        }
    }
}
