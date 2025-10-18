package io.monosense.synergyflow.common.infrastructure;

import io.monosense.synergyflow.common.events.CorrelationContext;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that extracts or generates correlation ID from incoming requests.
 *
 * <p>Correlation ID is:
 * <ul>
 *   <li>Extracted from X-Correlation-ID header if present</li>
 *   <li>Generated as new UUID if header missing</li>
 *   <li>Stored in MDC for logging</li>
 *   <li>Added to OpenTelemetry span attributes</li>
 *   <li>Returned in response headers</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract or generate correlation ID
            String correlationId = httpRequest.getHeader(CorrelationContext.HEADER_CORRELATION_ID);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = CorrelationContext.generateCorrelationId();
            }

            // Set in MDC for logging
            CorrelationContext.setCorrelationId(correlationId);

            // Set causation ID to correlation ID for user-initiated requests
            CorrelationContext.setCausationId(correlationId);

            // Add to OpenTelemetry span
            Span currentSpan = Span.current();
            currentSpan.setAttribute("correlation.id", correlationId);
            currentSpan.setAttribute("causation.id", correlationId);

            // Return correlation ID in response header
            httpResponse.setHeader(CorrelationContext.HEADER_CORRELATION_ID, correlationId);

            chain.doFilter(request, response);

        } finally {
            // Clear MDC to prevent leakage across requests
            CorrelationContext.clear();
        }
    }
}
