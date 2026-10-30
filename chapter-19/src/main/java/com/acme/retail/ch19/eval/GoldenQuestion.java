package com.acme.retail.ch19.eval;

import java.util.List;

/**
 * One entry in the golden dataset: a real customer-style question about a
 * known product, plus the phrases that would prove the answer is grounded
 * in the actual inventory record (and the phrases that would prove it is
 * not).
 *
 * <p>A "golden dataset" is a small, hand-curated set of question/answer
 * pairs whose correct answer is already known. It is the AI-evaluation
 * equivalent of a fixture in a normal test -- except the thing being
 * checked is a scored judgment, not an exact string.
 */
public record GoldenQuestion(
        String id,
        String question,
        String productSku,
        String productName,
        List<String> acceptablePhrases,
        List<String> contradictingPhrases) {
}
