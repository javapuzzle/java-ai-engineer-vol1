package com.acme.retail.ch14.visualsearch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogVisualContextProviderTest {

    private final CatalogVisualContextProvider provider = new CatalogVisualContextProvider();

    @Test
    void summary_listsOnlyRealCategoriesAndColors() {
        String summary = provider.summary();

        assertThat(summary).contains("Running Shoes", "Hiking Boots", "Backpacks", "Accessories");
        assertThat(summary).contains("Black", "Navy");
    }

    @Test
    void summary_instructsHonestyWhenNoMatch() {
        assertThat(provider.summary()).contains("say so honestly instead of guessing");
    }
}
