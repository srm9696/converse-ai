package com.sachin.converse_ai.dto;

import com.sachin.converse_ai.dao.MessageRole;
import java.time.Instant;

public record ConversationHistoryItem(MessageRole role, String content, Instant createdAt) {}
