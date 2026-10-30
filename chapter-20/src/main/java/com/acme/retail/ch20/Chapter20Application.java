package com.acme.retail.ch20;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 20 -- Launch Day.
 *
 * <p>This is the entry point for the capstone module. Starting it boots the
 * AI shopping assistant behind three things working together: a circuit
 * breaker with fallback (Chapter 16), structured logs and metrics
 * (Chapter 17), and a feature flag that only re-opens after a golden-dataset
 * evaluation (Chapter 19) confirms a fix is safe.
 *
 * <p>None of these ideas are new in this chapter. What is new is that Sarah
 * now reaches for all of them, in order, without panic -- that is the whole
 * point of the chapter.
 */
@SpringBootApplication
public class Chapter20Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter20Application.class, args);
    }
}
