package com.acme.retail.ch17;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 17 -- Something Is Wrong, But Nobody Knows What.
 *
 * <p>This module does not introduce a new feature for the customer. It
 * introduces a new capability for Sarah and Alex: the ability to reconstruct
 * exactly what happened during any single request to the AI assistant, using
 * a correlation ID, structured logs, and Micrometer metrics.
 */
@SpringBootApplication
public class Chapter17Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter17Application.class, args);
    }
}
