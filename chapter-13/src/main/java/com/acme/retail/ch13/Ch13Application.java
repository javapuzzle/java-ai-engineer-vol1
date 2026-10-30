package com.acme.retail.ch13;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 13 -- "Sarah Forgot the Customer."
 *
 * <p>Boots the shopping assistant: an HTTP API in front of an LLM (Large
 * Language Model), with conversation history explicitly loaded from Redis
 * and injected into every call. See {@code assistant.ShoppingAssistantService}
 * for where that happens.
 */
@SpringBootApplication
public class Ch13Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch13Application.class, args);
    }
}
