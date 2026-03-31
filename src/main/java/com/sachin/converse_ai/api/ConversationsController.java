package com.sachin.converse_ai.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
			@RequestHeader(HEADER_API_KEY) String apiKey,
			@RequestHeader(HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
			@Valid @RequestBody ConversationMessageRequest request) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@PostMapping("/{conversationId}/messages")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	public ConversationMessageResponse appendMessage(
			@RequestHeader(HEADER_API_KEY) String apiKey,
			@RequestHeader(HEADER_IDEMPOTENCY_KEY) String idempotencyKey,
			@PathVariable UUID conversationId,
			@Valid @RequestBody ConversationMessageRequest request) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@GetMapping("/{conversationId}/messages")
	@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
	public List<ConversationHistoryItem> getHistory(
			@RequestHeader(HEADER_API_KEY) String apiKey, @PathVariable UUID conversationId) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
}

