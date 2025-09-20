package io.monosense.synergyflow.user.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Ensures every request/response pair carries a correlation identifier for tracing.
 * Propagates an incoming {@value #CORRELATION_ID_HEADER} header when present, otherwise
 * generates a fresh identifier and exposes it on the response while binding it to the MDC.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String correlationId = extractCorrelationId(request);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put("correlationId", correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String headerValue = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(headerValue)) {
            return headerValue.trim();
        }
        return UUID.randomUUID().toString();
    }
}
