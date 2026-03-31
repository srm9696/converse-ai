package com.sachin.converse_ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationMessageRequest(@NotBlank String message) {}
