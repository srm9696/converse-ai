package com.sachin.converse_ai.client;

public interface LlmClient {

	LlmCompletionResult complete(LlmChatCommand command);
}
