package com.acme.retail.ch10.assistant;

import org.springframework.stereotype.Component;

/**
 * Answers Alex's question from this chapter: "What exactly should the AI
 * know?"
 *
 * <p>This is a deliberately small, deliberately static answer. It returns a
 * short, human-written summary of Acme Retail's catalog categories -- not a
 * live query against the product database. That keeps this chapter's scope
 * honest: Sarah is learning how to hand context to a model at all, not yet
 * how to retrieve live, per-product data for it (that is Chapter 11's
 * retrieval-augmented generation problem).
 *
 * <p>In a real system, this class is the seam where "what the AI is allowed
 * to know" gets decided. Today it is a hardcoded String. Later chapters
 * replace it with something that queries live systems -- but the decision
 * about what context to hand the model belongs here, not inside the prompt
 * text itself.
 */
@Component
public class CatalogContextProvider {

    public String summary() {
        return """
                Acme Retail sells outdoor and athletic gear, organized into these
                categories:
                - Running Shoes: road and trail running shoes for all experience levels.
                - Hiking Boots: waterproof and non-waterproof boots for day hikes and
                  multi-day trips.
                - Backpacks: daypacks and multi-day hiking packs, 15 to 65 liters.
                - Accessories: socks, insoles, hydration bottles, and trail maps.
                """;
    }
}
