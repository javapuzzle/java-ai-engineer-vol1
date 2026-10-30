package com.acme.retail.ch14.visualsearch;

/**
 * @param question      the customer's text question about the photo, e.g.
 *                       "Do you have this in another color?"
 * @param imageBytes     the raw bytes of the uploaded photo
 * @param imageMimeType  the photo's MIME type, e.g. "image/jpeg"
 */
public record VisualSearchRequest(String question, byte[] imageBytes, String imageMimeType) {
}
