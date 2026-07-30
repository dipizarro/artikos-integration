package cl.atk.nomina.batch.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayHeadersLoggingFilter extends OncePerRequestFilter {

    static final String CORRELATION_ID = "correlationId";
    static final String REQUEST_ID = "requestId";
    static final String CLIENT_ID = "clientId";
    static final String CONSUMER = "consumer";
    static final String FORWARDED_FOR = "forwardedFor";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        putIfPresent(CORRELATION_ID, request.getHeader("X-Correlation-Id"));
        putIfPresent(REQUEST_ID, request.getHeader("X-Request-Id"));
        putIfPresent(CLIENT_ID, request.getHeader("X-Client-Id"));
        putIfPresent(CONSUMER, request.getHeader("X-Consumer-Username"));
        putIfPresent(FORWARDED_FOR, request.getHeader("X-Forwarded-For"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
            MDC.remove(REQUEST_ID);
            MDC.remove(CLIENT_ID);
            MDC.remove(CONSUMER);
            MDC.remove(FORWARDED_FOR);
        }
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
