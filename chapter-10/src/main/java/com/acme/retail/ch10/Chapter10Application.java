package com.acme.retail.ch10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 10 -- Sarah Meets AI.
 *
 * <p>This is the entry point for the shopping assistant Sarah builds when
 * Maya asks Acme Retail's website to help customers find products using
 * plain language instead of search filters. Starting it boots a normal
 * Spring Boot web application, plus an auto-configured Spring AI
 * {@code ChatClient.Builder} bean wired to an OpenAI-compatible chat model.
 */
@SpringBootApplication
public class Chapter10Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter10Application.class, args);
    }
}
