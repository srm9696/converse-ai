package com.sachin.converse_ai.exception;

import com.sachin.converse_ai.client.LlmInvocationFailedException;
import com.sachin.converse_ai.dto.ErrorResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.concurrent.TimeoutException;

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

	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of("IDEMPOTENCY_CONFLICT", ex.getMessage()));
	}

	@ExceptionHandler(IdempotencyFailedKeyException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyFailedKey(IdempotencyFailedKeyException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of("IDEMPOTENCY_FAILED_KEY", ex.getMessage()));
	}

	@ExceptionHandler(LlmInvocationFailedException.class)
	public ResponseEntity<ErrorResponse> handleLlmFailure(LlmInvocationFailedException ex) {
		if (findCause(ex, TimeoutException.class) != null) {
			return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
					.body(ErrorResponse.of("LLM_TIMEOUT", "Assistant response timed out"));
		}
		if (findCause(ex, CallNotPermittedException.class) != null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(ErrorResponse.of("LLM_CIRCUIT_OPEN", "Assistant is temporarily unavailable"));
		}
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(ErrorResponse.of("LLM_INVOCATION_FAILED", "Assistant could not generate a response"));
	}

	private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
		Throwable current = ex;
		while (current != null) {
			if (type.isInstance(current)) {
				return type.cast(current);
			}
			current = current.getCause();
		}
		return null;
	}
}
