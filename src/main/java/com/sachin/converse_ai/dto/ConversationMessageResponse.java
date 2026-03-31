package com.sachin.converse_ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationMessageResponse(
		UUID conversationId, UUID assistantMessageId, String assistantMessage, Instant createdAt) {}
