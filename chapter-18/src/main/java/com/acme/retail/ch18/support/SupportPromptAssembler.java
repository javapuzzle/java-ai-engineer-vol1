package com.acme.retail.ch18.support;

import com.acme.retail.ch18.customer.Customer;
import com.acme.retail.ch18.order.Order;
import com.acme.retail.ch18.security.OrderStatusView;
import com.acme.retail.ch18.security.PiiRedactionService;
import com.acme.retail.ch18.security.RedactedCustomer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the context block that gets placed into the shopping
 * assistant's prompt, alongside the customer's chat message.
 *
 * <p><b>This is the fix.</b> Before this class existed, the code that
 * assembled the prompt looked roughly like this (do not copy this -- it
 * is the bug, shown here only so its shape is recognizable):
 *
 * <pre>{@code
 * // THE BUG -- do not reintroduce this.
 * String context = "Customer: " + customer.getFullName()
 *         + " (" + customer.getEmail() + ", " + customer.getPhone() + ")"
 *         + ", tier=" + customer.getLoyaltyTier();
 * }</pre>
 *
 * <p>That line passed code review because it "just" builds a string for
 * the model to read -- nobody was thinking about the fact that the whole
 * prompt, including that string, gets logged and sent to a third-party
 * AI provider. {@link PiiRedactionService} makes it impossible to reach
 * {@code customer.getEmail()} from this class at all: it only ever
 * hands out a {@link RedactedCustomer}.
 */
@Component
public class SupportPromptAssembler {

    private final PiiRedactionService piiRedactionService;

    public SupportPromptAssembler(PiiRedactionService piiRedactionService) {
        this.piiRedactionService = piiRedactionService;
    }

    /**
     * Builds the safe context block for the assistant's prompt: a
     * redacted customer view plus redacted order views. No email, no
     * phone number, no payment detail ever appears in the returned
     * string.
     */
    public String buildContextBlock(Customer customer, List<Order> recentOrders) {
        RedactedCustomer redactedCustomer = piiRedactionService.redactForPrompt(customer);

        StringBuilder context = new StringBuilder();
        context.append("Customer: ").append(redactedCustomer.displayName())
                .append(" (loyalty tier: ").append(redactedCustomer.loyaltyTier()).append(")\n");

        context.append("Recent orders:\n");
        for (Order order : recentOrders) {
            OrderStatusView view = piiRedactionService.redactForPrompt(order);
            context.append("- ").append(view.orderCode())
                    .append(": ").append(view.status())
                    .append(", estimated delivery ").append(view.estimatedDelivery())
                    .append("\n");
        }

        return context.toString();
    }
}
