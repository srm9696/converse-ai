package com.sachin.converse_ai.api;

import java.time.Instant;
import java.util.UUID;

public record ConversationMessageResponse(
		UUID conversationId, UUID assistantMessageId, String assistantMessage, Instant createdAt) {}

