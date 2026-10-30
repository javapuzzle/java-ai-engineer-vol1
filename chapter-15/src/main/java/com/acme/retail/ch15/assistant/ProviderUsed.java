package com.acme.retail.ch15.assistant;

/**
 * Which provider actually answered a request -- logged and returned so an
 * incident, or a test, can prove the fallback path really executed, instead
 * of only asserting that it theoretically could.
 */
public enum ProviderUsed {
    PRIMARY,
    FALLBACK
}
