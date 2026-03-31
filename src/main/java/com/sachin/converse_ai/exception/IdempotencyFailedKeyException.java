package com.sachin.converse_ai.exception;

public class IdempotencyFailedKeyException extends RuntimeException {

	public IdempotencyFailedKeyException(String message) {
		super(message);
	}
}
