package com.acme.retail.ch19.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A classic deterministic unit test: fixed input in, fixed expected output
 * out. No Spring context, no database, no network call.
 *
 * <p>{@link GroundednessScorer} is pure logic, so it can be tested exactly
 * the way Sarah tested {@code ProductService} back in Chapter 01. The
 * interesting part of Chapter 19 is not this test -- it is that a test
 * shaped exactly like this one CANNOT tell Sarah whether a real model's
 * wording is correct. That needs a different kind of test, further down
 * in this module's {@code src/test} tree.
 */
class GroundednessScorerTest {

    private final GoldenQuestion question = new GoldenQuestion(
            "Q1", "Is it in stock?", "SHOE-BLK-42", "Trail Runner 42",
            List.of("sold out"), List.of("is in stock"));

    @Test
    void answerIsGroundedAndRelevantWhenItMatchesTheKnownFact() {
        EvaluationResult result = GroundednessScorer.score(
                question, "The Trail Runner 42 is sold out right now.");

        assertThat(result.grounded()).isTrue();
        assertThat(result.relevant()).isTrue();
    }

    @Test
    void answerCanBeRelevantWithoutBeingGrounded() {
        // This is the exact shape of the bug that opened this chapter: the
        // AI names the right product, but confidently states the wrong
        // stock fact. A test that only checked "did it mention the shoe?"
        // would have missed this.
        EvaluationResult result = GroundednessScorer.score(
                question, "Yes, the Trail Runner 42 is in stock and ships today.");

        assertThat(result.relevant()).isTrue();
        assertThat(result.grounded()).isFalse();
    }

    @Test
    void answerIsNotRelevantWhenItNeverAddressesTheProduct() {
        EvaluationResult result = GroundednessScorer.score(
                question, "Our store is open Monday to Friday, 9am to 5pm.");

        assertThat(result.relevant()).isFalse();
    }
}
