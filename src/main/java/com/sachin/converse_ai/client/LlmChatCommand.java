package com.sachin.converse_ai.client;

import java.util.UUID;

public record LlmChatCommand(UUID conversationId, String userMessage) {}
