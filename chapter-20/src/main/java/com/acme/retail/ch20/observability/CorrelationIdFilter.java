package com.acme.retail.ch20.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.UUID;

/**
 * Attaches a correlation ID to every request, the way Chapter 17 does.
 *
 * <p>A correlation ID is a single identifier that ties one customer
 * request to every log line, metric, and trace span it produces --
 * without it, "find everything that happened for this one request" means
 * grepping timestamps and hoping. This filter reads {@code
 * X-Correlation-Id} from the incoming request if the caller already has
 * one (for example, from an upstream gateway), or generates a new one, and
 * puts it in SLF4J's {@link MDC} (Mapped Diagnostic Context) so every log
 * statement for the rest of this request automatically includes it.
 */
@Component
public class CorrelationIdFilter extends GenericFilterBean {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
