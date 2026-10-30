package com.acme.retail.ch18;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 18 -- The AI Should Never Have Seen That.
 *
 * <p>This is the entry point for the secured version of Acme Retail's
 * shopping assistant. The customer and order data still flow into the
 * assistant's prompt, but only after passing through
 * {@link com.acme.retail.ch18.security.PiiRedactionService}, and the
 * assistant's only function-calling tool is
 * {@link com.acme.retail.ch18.support.OrderStatusLookupTool} -- a single,
 * read-only, input-validated method instead of direct database access.
 */
@SpringBootApplication
public class Chapter18Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter18Application.class, args);
    }
}
