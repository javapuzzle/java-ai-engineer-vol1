package com.acme.retail.ch18.security;

/**
 * The only view of a customer that is safe to put in an AI prompt.
 *
 * <p>Compare this to {@link com.acme.retail.ch18.customer.Customer}: no
 * email, no phone number. Just enough for the assistant to sound
 * personal ({@code displayName}) and apply loyalty-tier business rules
 * ({@code loyaltyTier}), and nothing an attacker or a careless log
 * statement could use to identify or contact the customer.
 */
public record RedactedCustomer(Long customerId, String displayName, String loyaltyTier) {
}
