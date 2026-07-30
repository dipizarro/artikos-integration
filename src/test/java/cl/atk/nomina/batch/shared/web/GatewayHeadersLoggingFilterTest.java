package cl.atk.nomina.batch.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GatewayHeadersLoggingFilterTest {

    private final GatewayHeadersLoggingFilter filter = new GatewayHeadersLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doesNotFailWhenGatewayHeadersAreMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("correlationId")).isNull();
            assertThat(MDC.get("requestId")).isNull();
            assertThat(MDC.get("clientId")).isNull();
            assertThat(MDC.get("consumer")).isNull();
            assertThat(MDC.get("forwardedFor")).isNull();
        });
    }

    @Test
    void addsGatewayHeadersToMdcDuringRequestAndClearsThemAfterwards() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-1");
        request.addHeader("X-Request-Id", "req-1");
        request.addHeader("X-Client-Id", "client-1");
        request.addHeader("X-Consumer-Username", "consumer-1");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        request.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("corr-1");
            assertThat(MDC.get("requestId")).isEqualTo("req-1");
            assertThat(MDC.get("clientId")).isEqualTo("client-1");
            assertThat(MDC.get("consumer")).isEqualTo("consumer-1");
            assertThat(MDC.get("forwardedFor")).isEqualTo("10.0.0.1");
            assertThat(MDC.get("Authorization")).isNull();
            assertThat(MDC.get("authorization")).isNull();
        });

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("clientId")).isNull();
        assertThat(MDC.get("consumer")).isNull();
        assertThat(MDC.get("forwardedFor")).isNull();
    }
}
