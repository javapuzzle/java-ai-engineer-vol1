package com.acme.retail.ch18.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A customer order.
 *
 * <p>{@code orderCode} is the business-facing identifier customers and
 * support staff use (for example {@code "ORD-104822"}) -- it is what a
 * customer types into a chat message, and what the AI assistant's tool
 * accepts as input. {@code paymentLast4} is a partial payment detail (the
 * last four digits of the card used to pay) -- still sensitive enough
 * that it must never be handed to the assistant's prompt or returned from
 * a tool call. See {@link com.acme.retail.ch18.security.PiiRedactionService}.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    private String orderCode;

    private String status;

    private LocalDate estimatedDelivery;

    private String paymentLast4;

    protected Order() {
        // Required by JPA/Hibernate to build entities via reflection.
    }

    public Order(Long customerId, String orderCode, String status,
                 LocalDate estimatedDelivery, String paymentLast4) {
        this.customerId = customerId;
        this.orderCode = orderCode;
        this.status = status;
        this.estimatedDelivery = estimatedDelivery;
        this.paymentLast4 = paymentLast4;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public String getPaymentLast4() {
        return paymentLast4;
    }
}
