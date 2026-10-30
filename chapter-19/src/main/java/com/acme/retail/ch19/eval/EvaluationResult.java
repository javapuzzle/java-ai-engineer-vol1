package com.acme.retail.ch19.eval;

/**
 * The result of scoring one answer against one {@link GoldenQuestion}.
 *
 * <p>{@code grounded} and {@code relevant} are deliberately separate
 * booleans, not one combined "correct" flag. An answer can mention the
 * right product and still state the wrong fact (relevant, not grounded)
 * -- that is exactly the bug that started this chapter.
 */
public record EvaluationResult(String questionId, String answer, boolean grounded, boolean relevant) {

    public boolean passed() {
        return grounded && relevant;
    }
}
