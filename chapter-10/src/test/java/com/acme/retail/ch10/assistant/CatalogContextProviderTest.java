package com.acme.retail.ch10.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogContextProviderTest {

    @Test
    void summaryDescribesCatalogCategoriesWithoutMentioningLiveStock() {
        String summary = new CatalogContextProvider().summary();

        assertThat(summary)
                .isNotBlank()
                .contains("Running Shoes")
                .contains("Hiking Boots")
                .contains("Backpacks");

        // The model should only ever see category-level descriptions here,
        // never live stock numbers -- that boundary is the point of this
        // chapter's Production Notes, and it is what Chapter 11 tests next.
        assertThat(summary).doesNotContainIgnoringCase("stock");
    }
}
