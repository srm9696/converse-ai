package com.sachin.converse_ai.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ResilientLlmClient implements LlmClient {

	private static final String RESILIENCE_NAME = "llmClient";

	private static final Logger log = LoggerFactory.getLogger(ResilientLlmClient.class);

	private final MockLlmClient delegate;
	private final CircuitBreaker circuitBreaker;
	private final Retry retry;
	private final TimeLimiter timeLimiter;
	private final ScheduledExecutorService timeLimiterScheduler;

	public ResilientLlmClient(
			MockLlmClient delegate,
			CircuitBreakerRegistry circuitBreakerRegistry,
			RetryRegistry retryRegistry,
			TimeLimiterRegistry timeLimiterRegistry) {
		this.delegate = delegate;
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_NAME);
		this.retry = retryRegistry.retry(RESILIENCE_NAME);
		this.timeLimiter = timeLimiterRegistry.timeLimiter(RESILIENCE_NAME);
		this.timeLimiterScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "llm-timelimiter");
			thread.setDaemon(true);
			return thread;
		});

		this.retry
				.getEventPublisher()
				.onRetry(event ->
						log.warn(
								"llm_retry conversationId={} attempt={} waitInterval={}",
								"n/a",
								event.getNumberOfRetryAttempts(),
								event.getWaitInterval()));
	}

	@Override
	public LlmCompletionResult complete(LlmChatCommand command) {
		Supplier<CompletionStage<LlmCompletionResult>> resilientStage =
				() ->
						CompletableFuture.supplyAsync(
								Decorators.ofSupplier(() -> delegate.complete(command))
										.withCircuitBreaker(circuitBreaker)
										.withRetry(retry)
										.decorate());

		try {
			return timeLimiter
					.executeCompletionStage(timeLimiterScheduler, resilientStage)
					.toCompletableFuture()
					.join();
		} catch (CallNotPermittedException e) {
			log.warn("llm_circuit_open conversationId={}", command.conversationId());
			throw new LlmInvocationFailedException("LLM circuit breaker is open", e);
		} catch (CompletionException e) {
			Throwable cause = e.getCause() == null ? e : e.getCause();
			if (cause instanceof TimeoutException) {
				log.warn("llm_timeout conversationId={}", command.conversationId());
				throw new LlmInvocationFailedException("LLM call timed out", cause);
			}
			throw new LlmInvocationFailedException("LLM call failed", cause);
		}
	}
}
