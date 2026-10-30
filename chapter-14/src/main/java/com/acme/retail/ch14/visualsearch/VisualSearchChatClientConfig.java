package com.acme.retail.ch14.visualsearch;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VisualSearchChatClientConfig {

    @Bean
    ChatClient visualSearchChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
