package com.acme.retail.ch17.correlation;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenCallerSendsNone() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assistant/messages");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
    }

    @Test
    void reusesTheCorrelationIdTheCallerAlreadySent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assistant/messages");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "ticket-4821");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("ticket-4821");
    }

    @Test
    void clearsTheMdcAfterTheRequestFinishes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assistant/messages");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "ticket-4821");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // A reused thread must never leak one request's correlation ID
        // into the next, unrelated request.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
