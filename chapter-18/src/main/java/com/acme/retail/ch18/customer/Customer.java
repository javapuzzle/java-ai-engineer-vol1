package com.acme.retail.ch18.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A customer in the Acme Retail system.
 *
 * <p>This is the entity that caused the incident in this chapter. {@code
 * email} and {@code phone} are PII (Personally Identifiable Information --
 * any data that can identify a specific real person, such as an email
 * address, a phone number, or a home address). Nothing in this class
 * stops a developer from passing a whole {@code Customer} object into a
 * prompt -- that check has to happen somewhere else, on purpose. See
 * {@link com.acme.retail.ch18.security.PiiRedactionService}.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    private String email;

    private String phone;

    private String loyaltyTier;

    protected Customer() {
        // Required by JPA/Hibernate to build entities via reflection.
    }

    public Customer(String fullName, String email, String phone, String loyaltyTier) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.loyaltyTier = loyaltyTier;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLoyaltyTier() {
        return loyaltyTier;
    }
}
