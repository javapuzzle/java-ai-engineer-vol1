package com.acme.retail.ch17.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a correlation ID to every incoming request.
 *
 * <p>If the caller already sent one (for example, a support tool re-playing
 * a customer's ticket number), we reuse it so the whole call chain can be
 * tied back to that one ticket. Otherwise we generate a new one.
 *
 * <p>The ID is stored in SLF4J's MDC (Mapped Diagnostic Context) so that
 * every log line written while handling this request -- no matter which
 * class writes it -- automatically includes the same correlation ID. It is
 * also echoed back as a response header, so a client or support tool can
 * quote it back to us later.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the MDC. Without this, on a reused thread the
            // next unrelated request could accidentally log the wrong
            // correlation ID.
            MDC.remove(MDC_KEY);
        }
    }
}
