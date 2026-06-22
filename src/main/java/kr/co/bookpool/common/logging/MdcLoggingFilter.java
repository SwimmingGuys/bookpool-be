package kr.co.bookpool.common.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MdcLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(MdcLoggingFilter.class);

	private static final String TRACE_ID = "traceId";
	private static final String USER_ID = "userId";
	private static final String REQUEST_METHOD = "httpMethod";
	private static final String REQUEST_URI = "httpUri";
	private static final String TRACE_ID_HEADER = "X-Request-Id";

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain chain
	) throws ServletException, IOException {
		String traceId = request.getHeader(TRACE_ID_HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString().replace("-", "");
		}

		long start = System.currentTimeMillis();
		try {
			MDC.put(TRACE_ID, traceId);
			MDC.put(REQUEST_METHOD, request.getMethod());
			MDC.put(REQUEST_URI, request.getRequestURI());
			response.setHeader(TRACE_ID_HEADER, traceId);

			chain.doFilter(request, response);
		} finally {
			long elapsed = System.currentTimeMillis() - start;
			log.info("{} {} -> {} ({} ms)",
				request.getMethod(),
				request.getRequestURI(),
				response.getStatus(),
				elapsed);

			MDC.remove(TRACE_ID);
			MDC.remove(USER_ID);
			MDC.remove(REQUEST_METHOD);
			MDC.remove(REQUEST_URI);
		}
	}
}
