package com.acme.retail.ch10.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ShoppingAssistantController}.
 *
 * <p>This test never calls a real AI provider. It mocks {@link ChatClient}
 * with Mockito's "deep stubs" so the fluent chain
 * {@code chatClient.prompt().system(...).user(...).call().entity(...)}
 * can be stubbed in one go, and checks that the controller returns whatever
 * structured {@link ProductRecommendation} the (mocked) model produced.
 * This is possible only because the response is a typed record and not raw
 * text -- see the Code Explanation in the chapter for why that matters for
 * testing.
 */
class ShoppingAssistantControllerTest {

    @Test
    void recommendReturnsTheStructuredRecommendationProducedByTheChatClient() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ProductRecommendation expected = new ProductRecommendation(
                "Trailblazer GTX",
                "Hiking Boots",
                "Waterproof and durable, a good fit for regular day hikes.");

        when(chatClient.prompt()
                .system(any(String.class))
                .user(any(String.class))
                .call()
                .entity(ProductRecommendation.class))
                .thenReturn(expected);

        ShoppingAssistantController controller =
                new ShoppingAssistantController(chatClient, new CatalogContextProvider());

        ProductRecommendation actual =
                controller.recommend(new ShoppingAssistantRequest("I need boots for weekend day hikes"));

        assertThat(actual).isEqualTo(expected);
    }
}
