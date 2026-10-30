package com.acme.retail.ch20.evaluation;

/**
 * One row of the golden dataset: a question about a product, and the
 * correct availability answer for it.
 *
 * <p>This is Chapter 19's idea applied to this chapter's incident: a
 * small, fixed set of question/answer pairs whose correct answers are
 * known in advance, so a fix can be graded automatically instead of by
 * eyeballing a few manual test prompts.
 *
 * @param id               a short identifier for the case, used in
 *                          failure reports
 * @param sku               the product the question is about
 * @param question          the customer question to ask the model
 * @param expectedInStock   the correct availability answer
 */
public record GoldenCase(String id, String sku, String question, boolean expectedInStock) {
}
