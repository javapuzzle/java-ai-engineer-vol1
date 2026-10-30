package com.acme.retail.ch15.assistant;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds two distinct, explicitly-named {@code ChatClient} beans -- one per
 * provider -- instead of the single, implicit bean Chapter 10 relied on.
 *
 * <p>With both the OpenAI and Anthropic starters on the classpath at once,
 * Spring Boot auto-configures a {@code ChatModel} bean for each provider.
 * Naming each resulting {@code ChatClient} bean explicitly (rather than
 * exposing exactly one unnamed bean, as every earlier chapter did) is what
 * avoids the ambiguous auto-configuration Chapter 09 warned about.
 */
@Configuration
public class ProviderChatClientConfig {

    @Bean
    ChatClient primaryChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean
    ChatClient fallbackChatClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel).build();
    }
}
