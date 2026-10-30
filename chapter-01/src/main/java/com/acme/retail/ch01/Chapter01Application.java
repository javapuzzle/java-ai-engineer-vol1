package com.acme.retail.ch01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chapter 01 -- Sarah's First Day at Acme Retail.
 *
 * <p>This is the entry point for the product API Sarah builds on her first
 * day. Starting it boots a normal layered Spring Boot application:
 * ProductController -&gt; ProductService -&gt; ProductRepository -&gt; PostgreSQL.
 */
@SpringBootApplication
public class Chapter01Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter01Application.class, args);
    }
}
