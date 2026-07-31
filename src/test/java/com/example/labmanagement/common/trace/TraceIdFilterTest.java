package com.example.labmanagement.common.trace;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

	private final TraceIdFilter filter = new TraceIdFilter();

	@Test
	void keepsValidIncomingTraceIdDuringRequestAndClearsMdcAfterward() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "caller.trace-1");

		filter.doFilter(request, response, (servletRequest, servletResponse) -> {
			assertThat(TraceContext.currentTraceId()).isEqualTo("caller.trace-1");
			assertThat(servletRequest.getAttribute(TraceContext.TRACE_ID)).isEqualTo("caller.trace-1");
		});

		assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("caller.trace-1");
		assertThat(MDC.get(TraceContext.TRACE_ID)).isNull();
	}

	@Test
	void replacesUnsafeIncomingTraceId() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "unsafe\nvalue");

		filter.doFilter(request, response, (servletRequest, servletResponse) -> {
		});

		assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER))
				.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	}
}
