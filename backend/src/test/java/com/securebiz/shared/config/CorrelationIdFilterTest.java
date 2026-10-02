package com.securebiz.shared.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesSafeClientCorrelationIdDuringRequestAndClearsMdcAfterwards() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "demo-request_42");
        var response = new MockHttpServletResponse();
        var observed = new AtomicReference<String>();

        filter.doFilter(request, response, (req, res) -> observed.set(MDC.get("correlationId")));

        assertThat(observed.get()).isEqualTo("demo-request_42");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("demo-request_42");
        assertThat(request.getAttribute(CorrelationIdFilter.ATTRIBUTE)).isEqualTo("demo-request_42");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void replacesUnsafeCorrelationIdToPreventHeaderAndLogInjection() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "bad\r\nforged-header: value");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
