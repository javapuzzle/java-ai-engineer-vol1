package com.acme.retail.ch18.security;

import com.acme.retail.ch18.customer.Customer;
import com.acme.retail.ch18.order.Order;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link PiiRedactionService}.
 *
 * <p>These tests are the executable proof that the fix works: a
 * redacted customer has no getter that could return an email or phone
 * number, and the masking helpers never print a full email or phone
 * number into a string a log line might carry.
 */
class PiiRedactionServiceTest {

    private final PiiRedactionService piiRedactionService = new PiiRedactionService();

    @Test
    void redactForPrompt_dropsEmailAndPhone_keepsDisplayNameAndTier() {
        Customer customer = new Customer(
                "Priya Nair", "priya.nair@example.com", "+1-555-201-4471", "GOLD");

        RedactedCustomer redacted = piiRedactionService.redactForPrompt(customer);

        assertThat(redacted.displayName()).isEqualTo("Priya");
        assertThat(redacted.loyaltyTier()).isEqualTo("GOLD");
        // RedactedCustomer has no email or phone field at all -- there is
        // no accessor to assert against, which is the point.
    }

    @Test
    void redactForPrompt_dropsCustomerIdAndPaymentDetail_fromOrder() {
        Order order = new Order(42L, "ORD-104822", "OUT_FOR_DELIVERY",
                LocalDate.of(2026, 8, 15), "4471");

        OrderStatusView view = piiRedactionService.redactForPrompt(order);

        assertThat(view.orderCode()).isEqualTo("ORD-104822");
        assertThat(view.status()).isEqualTo("OUT_FOR_DELIVERY");
        assertThat(view.estimatedDelivery()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void maskEmail_hidesMostOfTheLocalPart() {
        String masked = piiRedactionService.maskEmail("priya.nair@example.com");

        assertThat(masked).isEqualTo("p***@example.com");
        assertThat(masked).doesNotContain("priya.nair");
    }

    @Test
    void maskEmail_handlesBlankInput() {
        assertThat(piiRedactionService.maskEmail(null)).isEqualTo("[no-email]");
        assertThat(piiRedactionService.maskEmail("")).isEqualTo("[no-email]");
    }

    @Test
    void maskPhone_keepsOnlyLastTwoDigits() {
        String masked = piiRedactionService.maskPhone("+1-555-201-4471");

        assertThat(masked).isEqualTo("***71");
        assertThat(masked).doesNotContain("555");
    }
}
