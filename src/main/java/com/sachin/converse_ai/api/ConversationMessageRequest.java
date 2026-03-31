package com.sachin.converse_ai.api;

import jakarta.validation.constraints.NotBlank;

public record ConversationMessageRequest(@NotBlank String message) {}

