package com.sachin.converse_ai.logging;

import com.sachin.converse_ai.controller.ConversationsController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestDiagnosticFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			MDC.put("method", request.getMethod());
			MDC.put("path", request.getRequestURI());
			String idempotencyKey = request.getHeader(ConversationsController.HEADER_IDEMPOTENCY_KEY);
			if (idempotencyKey != null && !idempotencyKey.isBlank()) {
				MDC.put("idempotencyKey", idempotencyKey);
			}
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
