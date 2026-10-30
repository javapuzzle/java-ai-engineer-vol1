package com.acme.retail.ch03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 03 — When the Database Says No.
 *
 * <p>Sarah's Java service is no longer the bottleneck (that was Chapter 02).
 * Under real load, the database becomes the bottleneck: the HikariCP
 * connection pool maxes out, and the SQL log reveals an N+1 query pattern
 * hiding behind a perfectly normal-looking product catalog endpoint.
 */
@SpringBootApplication
public class Chapter03Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter03Application.class, args);
    }
}
