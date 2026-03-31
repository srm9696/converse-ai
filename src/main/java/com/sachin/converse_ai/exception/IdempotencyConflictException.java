package com.sachin.converse_ai.exception;

public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException(String message) {
		super(message);
	}
}
