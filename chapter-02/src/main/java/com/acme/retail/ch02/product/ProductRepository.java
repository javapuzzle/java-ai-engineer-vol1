package com.acme.retail.ch02.product;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Stands in for the real product database that chapter 03 covers in
 * detail.
 *
 * <p>The important thing about this class is not the data — it is a small
 * in-memory map — it is the {@link #simulateBlockingDatabaseCall()} call.
 * That one line is doing the same job a real JDBC call would do: it makes
 * the calling thread <b>block</b> (sit idle, doing nothing) while it waits
 * for a slow resource to respond. In production this is time spent
 * waiting on the network and on the database engine, not time spent
 * running Java code.
 *
 * <p>Every method here blocks for roughly {@link #SIMULATED_DATABASE_LATENCY_MS}
 * milliseconds, which is a realistic number for a moderately loaded
 * database under real traffic.
 */
@Repository
public class ProductRepository {

    /**
     * How long a "database call" pretends to take. This is the same
     * order of magnitude you would see for a query against a busy
     * PostgreSQL instance under real production load.
     */
    static final long SIMULATED_DATABASE_LATENCY_MS = 80;

    private final Map<Long, Product> products = new ConcurrentHashMap<>();

    public ProductRepository() {
        products.put(1L, new Product(1L, "Trail Running Shoe", "Grippy trail shoe, black/orange",
                8999, "SHOE-TRAIL-01", 42, "Footwear"));
        products.put(2L, new Product(2L, "Insulated Water Bottle", "750ml stainless steel bottle",
                2499, "BOTTLE-750-01", 130, "Outdoor"));
        products.put(3L, new Product(3L, "Trail Running Shoe (Wide)", "Wide-fit version of SHOE-TRAIL-01",
                9499, "SHOE-TRAIL-01-W", 15, "Footwear"));
    }

    public Product findById(Long id) {
        simulateBlockingDatabaseCall();
        Product product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    /**
     * This is the heart of the chapter. {@link Thread#sleep(long)} blocks
     * the current thread — it does not consume CPU, but the thread cannot
     * do anything else until the sleep finishes. A real blocking JDBC
     * call behaves the same way: the thread is parked, waiting for bytes
     * to come back over the network from the database.
     */
    private void simulateBlockingDatabaseCall() {
        try {
            Thread.sleep(SIMULATED_DATABASE_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting on the database", e);
        }
    }
}
