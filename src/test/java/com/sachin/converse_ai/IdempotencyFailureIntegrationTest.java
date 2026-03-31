package com.sachin.converse_ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyFailureIntegrationTest {

	private static final String TEST_API_KEY = "test-api-key-001";

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void mockLlmAlwaysFails(DynamicPropertyRegistry registry) {
		registry.add("mock.llm.failureRate", () -> "1");
		registry.add("mock.llm.minLatencyMs", () -> "0");
		registry.add("mock.llm.maxLatencyMs", () -> "0");
	}

	@Test
	void afterProcessingFailure_sameIdempotencyKeyIsRejectedUntilClientUsesANewKey() throws Exception {
		String idempotencyKey = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/conversations")
						.header("X-Api-Key", TEST_API_KEY)
						.header("Idempotency-Key", idempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isBadGateway());

		mockMvc.perform(post("/api/conversations")
						.header("X-Api-Key", TEST_API_KEY)
						.header("Idempotency-Key", idempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("IDEMPOTENCY_FAILED_KEY"));
	}
}
