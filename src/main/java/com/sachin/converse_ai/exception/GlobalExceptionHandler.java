package com.sachin.converse_ai.exception;

import com.sachin.converse_ai.client.LlmInvocationFailedException;
import com.sachin.converse_ai.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of("VALIDATION_ERROR", "Request validation failed"));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
	}

	@ExceptionHandler(LlmInvocationFailedException.class)
	public ResponseEntity<ErrorResponse> handleLlmFailure(LlmInvocationFailedException ex) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(ErrorResponse.of("LLM_INVOCATION_FAILED", "Assistant could not generate a response"));
	}
}
