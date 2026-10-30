package com.acme.retail.ch11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 11 -- The AI Is Wrong.
 *
 * <p>This module boots the Acme Retail shopping assistant that Sarah wired
 * up in Chapter 10, plus the fix she builds in this chapter: an
 * {@code InventoryLookupService} that retrieves real stock data and injects
 * it into the prompt as grounding context before the model ever generates
 * an answer.
 */
@SpringBootApplication
public class Chapter11Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter11Application.class, args);
    }
}
