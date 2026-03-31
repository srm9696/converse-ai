package com.sachin.converse_ai.security;

import com.sachin.converse_ai.controller.ConversationsController;
import com.sachin.converse_ai.dao.ApiKey;
import com.sachin.converse_ai.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

	private final ApiKeyRepository apiKeyRepository;
	private final PasswordEncoder passwordEncoder;

	public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
		this.apiKeyRepository = apiKeyRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (shouldBypass(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String apiKey = request.getHeader(ConversationsController.HEADER_API_KEY);
		if (apiKey == null || apiKey.isBlank()) {
			log.debug("Missing API key on {}", request.getRequestURI());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		List<ApiKey> candidates = apiKeyRepository.findAllActiveWithUser();
		for (ApiKey candidate : candidates) {
			if (passwordEncoder.matches(apiKey, candidate.getApiKeyHash())) {
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(
								candidate.getUser().getId(),
								apiKey,
								List.of(new SimpleGrantedAuthority("ROLE_USER")));
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
				log.debug("Authenticated userId={} path={}", candidate.getUser().getId(), request.getRequestURI());
				filterChain.doFilter(request, response);
				return;
			}
		}

		log.debug("Invalid API key on {}", request.getRequestURI());
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	private boolean shouldBypass(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri.startsWith("/h2-console");
	}
}
