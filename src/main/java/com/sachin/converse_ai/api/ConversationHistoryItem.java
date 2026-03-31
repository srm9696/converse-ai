package com.sachin.converse_ai.api;

import com.sachin.converse_ai.domain.MessageRole;
import java.time.Instant;

public record ConversationHistoryItem(MessageRole role, String content, Instant createdAt) {}

