package com.acme.retail.ch18.security;

import java.time.LocalDate;

/**
 * The only view of an order that {@code OrderStatusLookupTool} is allowed
 * to return to the AI model.
 *
 * <p>This is output validation in practice: even though
 * {@link com.acme.retail.ch18.order.Order} also carries {@code
 * customerId} and {@code paymentLast4}, this record physically cannot
 * carry them, because it does not declare those fields. The tool builds
 * one of these from an {@code Order} and throws the rest away before the
 * result ever reaches the model.
 */
public record OrderStatusView(String orderCode, String status, LocalDate estimatedDelivery) {
}
