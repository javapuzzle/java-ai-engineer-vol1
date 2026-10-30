package com.acme.retail.ch19.eval;

import com.acme.retail.ch19.product.Product;

import java.util.List;

/**
 * A small, hand-curated set of known-correct facts about three Acme Retail
 * products, used by {@code ShoppingAssistantEvaluationTest}.
 *
 * <p>Three products on purpose: one sold out, one comfortably in stock, one
 * low stock. Real golden datasets are larger, but a teaching example only
 * needs enough variety to show the scoring mechanics working correctly in
 * more than one situation.
 */
public final class GoldenDataset {

    private GoldenDataset() {
    }

    public static List<Product> products() {
        return List.of(
                new Product("Trail Runner 42", "Black trail running shoe",
                        8999, "SHOE-BLK-42", 0, "Footwear"),
                new Product("Storm Jacket", "Waterproof red jacket",
                        12999, "JCK-RED-08", 25, "Outerwear"),
                new Product("Commuter Backpack", "Grey 20L backpack",
                        5999, "BAG-GRY-01", 2, "Bags"));
    }

    public static List<GoldenQuestion> questions() {
        return List.of(
                new GoldenQuestion(
                        "Q1-sold-out",
                        "Is the black trail running shoe in stock?",
                        "SHOE-BLK-42",
                        "Trail Runner 42",
                        List.of("sold out", "out of stock", "no longer available", "0 units"),
                        List.of("is in stock", "we have plenty", "available for purchase")),
                new GoldenQuestion(
                        "Q2-in-stock",
                        "Can I buy the red storm jacket right now?",
                        "JCK-RED-08",
                        "Storm Jacket",
                        List.of("is in stock", "available for purchase", "25 unit"),
                        List.of("sold out", "out of stock")),
                new GoldenQuestion(
                        "Q3-low-stock",
                        "How many grey commuter backpacks are left?",
                        "BAG-GRY-01",
                        "Commuter Backpack",
                        List.of("only 2", "2 left", "2 unit"),
                        List.of("sold out", "out of stock")));
    }
}
