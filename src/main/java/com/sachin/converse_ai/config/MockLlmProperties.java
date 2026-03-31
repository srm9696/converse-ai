package com.sachin.converse_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.llm")
public class MockLlmProperties {

	/** Probability in \([0,1]\) that a call fails (for reliability testing). */
	private double failureRate;

	private long minLatencyMs;
	private long maxLatencyMs;

	public double getFailureRate() {
		return failureRate;
	}

	public void setFailureRate(double failureRate) {
		this.failureRate = failureRate;
	}

	public long getMinLatencyMs() {
		return minLatencyMs;
	}

	public void setMinLatencyMs(long minLatencyMs) {
		this.minLatencyMs = minLatencyMs;
	}

	public long getMaxLatencyMs() {
		return maxLatencyMs;
	}

	public void setMaxLatencyMs(long maxLatencyMs) {
		this.maxLatencyMs = maxLatencyMs;
	}
}
