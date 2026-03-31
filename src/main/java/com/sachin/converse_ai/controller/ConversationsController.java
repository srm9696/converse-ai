package com.sachin.converse_ai.controller;

import com.sachin.converse_ai.dto.ConversationHistoryItem;
import com.sachin.converse_ai.dto.ConversationMessageRequest;
import com.sachin.converse_ai.dto.ConversationMessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationsController {

	public static final String HEADER_API_KEY = "X-Api-Key";
	public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

	@PostMapping
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	public ConversationMessageResponse createConversation(
			@AuthenticationPrincipal UUID userId,
			@RequestHeader(HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
			@Valid @RequestBody ConversationMessageRequest request) {
		Objects.requireNonNull(userId, "userId");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@PostMapping("/{conversationId}/messages")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	public ConversationMessageResponse appendMessage(
			@AuthenticationPrincipal UUID userId,
			@RequestHeader(HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
			@PathVariable UUID conversationId,
			@Valid @RequestBody ConversationMessageRequest request) {
		Objects.requireNonNull(userId, "userId");
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@GetMapping("/{conversationId}/messages")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	public List<ConversationHistoryItem> getHistory(
			@AuthenticationPrincipal UUID userId, @PathVariable UUID conversationId) {
		Objects.requireNonNull(userId, "userId");
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
