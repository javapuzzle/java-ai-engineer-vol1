package com.acme.retail.ch20.evaluation;

import java.util.List;

/**
 * The outcome of running a golden dataset against the model provider.
 *
 * @param total          how many cases were run
 * @param passed         how many cases matched the expected answer
 * @param passRate       {@code passed / total}, or {@code 0.0} when
 *                       {@code total} is zero
 * @param failedCaseIds  the IDs of the cases that did not match, for a
 *                       human to look at directly
 */
public record EvaluationResult(int total, int passed, double passRate, List<String> failedCaseIds) {

    /**
     * Whether this result is good enough to allow a fix to go live.
     * {@code IncidentResponseService} calls this before re-enabling the
     * feature flag -- the gate the brief calls "verify with evaluation."
     */
    public boolean meetsGate(double passRateThreshold) {
        return passRate >= passRateThreshold;
    }
}
