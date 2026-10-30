package com.acme.retail.ch14.visualsearch;

import org.springframework.stereotype.Component;

/**
 * The same discipline as Chapter 11's {@code InventoryLookupService}: hand
 * the model a short, explicit list of the real categories and colors Acme
 * Retail actually sells, so a vision-capable model has something real to
 * match a photo against instead of guessing from general visual patterns.
 */
@Component
public class CatalogVisualContextProvider {

    public String summary() {
        return """
                Acme Retail's catalog uses these categories and colors only:
                - Categories: Running Shoes, Hiking Boots, Backpacks, Accessories.
                - Common colors carried across categories: Black, Grey, Navy,
                  Forest Green, Red, White.

                If the photo does not clearly match one of these categories or
                colors, say so honestly instead of guessing.
                """;
    }
}
