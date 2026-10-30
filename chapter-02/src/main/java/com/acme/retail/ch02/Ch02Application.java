package com.acme.retail.ch02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 02 — "It Works, But It's Slow".
 *
 * <p>This is the same kind of product API Sarah built in chapter 01, but
 * this module exists to reproduce one specific problem: what happens to a
 * Spring Boot API when every request has to block while waiting on a slow
 * downstream call (a database, in Sarah's case).
 *
 * <p>The fix that matters in this chapter lives in {@code application.yml}
 * ({@code spring.threads.virtual.enabled: true}), not in this class.
 */
@SpringBootApplication
public class Ch02Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch02Application.class, args);
    }
}
