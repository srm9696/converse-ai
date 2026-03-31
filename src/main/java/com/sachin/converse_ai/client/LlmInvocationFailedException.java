package com.sachin.converse_ai.client;

public class LlmInvocationFailedException extends RuntimeException {

	public LlmInvocationFailedException(String message) {
		super(message);
	}

	public LlmInvocationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
