package com.example.labmanagement.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";
	private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
		MDC.put(TraceContext.TRACE_ID, traceId);
		request.setAttribute(TraceContext.TRACE_ID, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TraceContext.TRACE_ID);
		}
	}

	private String resolveTraceId(String candidate) {
		if (candidate != null && VALID_TRACE_ID.matcher(candidate).matches()) {
			return candidate;
		}
		return UUID.randomUUID().toString();
	}
}
