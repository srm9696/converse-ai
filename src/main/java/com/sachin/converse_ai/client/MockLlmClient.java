package com.sachin.converse_ai.client;

import com.sachin.converse_ai.config.MockLlmProperties;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockLlmClient implements LlmClient {

	private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

	private final MockLlmProperties properties;

	public MockLlmClient(MockLlmProperties properties) {
		this.properties = properties;
	}

	@Override
	public LlmCompletionResult complete(LlmChatCommand command) {
		simulateLatency();
		maybeFail();

		String assistantText = "Mock assistant reply to: " + command.userMessage();
		log.debug(
				"mock_llm_complete conversationId={} assistantChars={}",
				command.conversationId(),
				assistantText.length());
		return new LlmCompletionResult(assistantText);
	}

	private void simulateLatency() {
		long min = Math.max(0L, properties.getMinLatencyMs());
		long max = Math.max(min, properties.getMaxLatencyMs());
		long sleepMillis = ThreadLocalRandom.current().nextLong(min, max + 1);
		try {
			Thread.sleep(sleepMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LlmInvocationFailedException("Interrupted during mock LLM latency simulation", e);
		}
	}

	private void maybeFail() {
		double rate = properties.getFailureRate();
		if (rate <= 0d) {
			return;
		}
		if (ThreadLocalRandom.current().nextDouble() < rate) {
			log.warn("mock_llm_simulated_failure");
			throw new LlmInvocationFailedException("Mock LLM simulated failure");
		}
	}
}
