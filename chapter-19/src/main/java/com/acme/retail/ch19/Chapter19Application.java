package com.acme.retail.ch19;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 19 -- Can We Trust Our Tests?
 *
 * <p>Boots the Acme Retail shopping assistant one more time, but this
 * chapter is not about a new feature. It is about how Sarah proves the
 * feature works -- and why "the test suite is green" and "the AI gives
 * correct answers" turned out to be two different questions.
 */
@SpringBootApplication
public class Chapter19Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter19Application.class, args);
    }
}
