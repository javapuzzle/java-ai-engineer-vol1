package com.acme.retail.ch19.eval;

import java.util.List;

/**
 * Scores one assistant answer against one golden question.
 *
 * <p>This class has no Spring dependency and makes no network call. It is
 * a plain function: text in, two booleans out. That is what makes it
 * possible to unit test the scoring logic itself with ordinary, fully
 * deterministic tests -- see {@code GroundednessScorerTest}.
 */
public final class GroundednessScorer {

    private GroundednessScorer() {
    }

    public static EvaluationResult score(GoldenQuestion question, String answer) {
        String normalized = answer == null ? "" : answer.toLowerCase();

        boolean grounded = containsAny(normalized, question.acceptablePhrases())
                && !containsAny(normalized, question.contradictingPhrases());

        boolean relevant = normalized.contains(question.productName().toLowerCase())
                || normalized.contains(question.productSku().toLowerCase());

        return new EvaluationResult(question.id(), answer, grounded, relevant);
    }

    private static boolean containsAny(String haystack, List<String> phrases) {
        return phrases.stream().anyMatch(phrase -> haystack.contains(phrase.toLowerCase()));
    }
}
