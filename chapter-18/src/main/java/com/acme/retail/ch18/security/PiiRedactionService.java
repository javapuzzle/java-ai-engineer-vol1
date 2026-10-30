package com.acme.retail.ch18.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Turns a full {@link com.acme.retail.ch18.customer.Customer} and its
 * sensitive fields into the redacted shapes that are safe to put into a
 * prompt, or safe to write into a log line.
 *
 * <p>This is the class Sarah writes after Daniel finds the leaked email
 * in the prompt log. Before this class existed, whoever built the prompt
 * context called {@code customer.getEmail()} directly. Nothing in the
 * compiler or the framework flagged that as a mistake -- PII redaction is
 * a rule the team has to enforce deliberately, in one place, so every
 * caller gets it for free instead of relying on every developer to
 * remember it every time.
 */
@Component
public class PiiRedactionService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^(.)[^@]*(@.+)$");

    /**
     * Produces the only customer shape that is allowed into a prompt.
     */
    public RedactedCustomer redactForPrompt(com.acme.retail.ch18.customer.Customer customer) {
        return new RedactedCustomer(
                customer.getId(),
                firstNameOnly(customer.getFullName()),
                customer.getLoyaltyTier());
    }

    /**
     * Produces the only order shape that is allowed into a prompt, or
     * returned from {@code OrderStatusLookupTool}.
     */
    public OrderStatusView redactForPrompt(com.acme.retail.ch18.order.Order order) {
        return new OrderStatusView(
                order.getOrderCode(),
                order.getStatus(),
                order.getEstimatedDelivery());
    }

    /**
     * Masks an email address for logging (for example, {@code
     * "sarah.chen@example.com"} becomes {@code "s***@example.com"}).
     *
     * <p>This is what Daniel's log line should have shown all along.
     * Masking for logs and redacting for prompts are two different jobs
     * with two different rules -- a log line can sometimes carry a
     * masked hint for debugging, while a prompt should not carry the
     * field at all.
     */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "[no-email]";
        }
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***$2");
    }

    /**
     * Masks a phone number, keeping only the last two digits (for
     * example {@code "+1-555-201-4471"} becomes {@code "***71"}).
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 2) {
            return "[no-phone]";
        }
        return "***" + phone.substring(phone.length() - 2);
    }

    private String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Customer";
        }
        int firstSpace = fullName.indexOf(' ');
        return firstSpace > 0 ? fullName.substring(0, firstSpace) : fullName;
    }
}
