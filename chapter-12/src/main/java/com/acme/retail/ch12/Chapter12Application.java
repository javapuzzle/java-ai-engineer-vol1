package com.acme.retail.ch12;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 12 -- "Customers Don't Want to Wait."
 *
 * <p>Boots the same shopping-assistant idea from earlier chapters, now with
 * two ways to answer a customer's question: a synchronous endpoint that
 * waits for the whole answer, and a streaming endpoint that sends the
 * answer to the browser one token at a time.
 */
@SpringBootApplication
public class Chapter12Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter12Application.class, args);
    }
}
