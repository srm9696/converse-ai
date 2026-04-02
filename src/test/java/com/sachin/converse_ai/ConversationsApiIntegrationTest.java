package com.sachin.converse_ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationsApiIntegrationTest {

	private static final String TEST_API_KEY = "test-api-key-001";

	@Autowired
	private MockMvc mockMvc;

	@DynamicPropertySource
	static void deterministicMockLlm(DynamicPropertyRegistry registry) {
		registry.add("mock.llm.failureRate", () -> "0");
		registry.add("mock.llm.minLatencyMs", () -> "0");
		registry.add("mock.llm.maxLatencyMs", () -> "0");
	}

	@Test
	void postWithoutApiKeyIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/conversations")
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createConversation_thenHistory_isOrderedUserThenAssistant() throws Exception {
		String idempotencyKey = UUID.randomUUID().toString();
		MvcResult created = mockMvc.perform(post("/api/conversations")
						.header("X-Api-Key", TEST_API_KEY)
						.header("Idempotency-Key", idempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.conversationId").exists())
				.andExpect(jsonPath("$.assistantMessageId").exists())
				.andReturn();

		String conversationId = readJsonString(created, "$.conversationId");

		mockMvc.perform(get("/api/conversations/{cid}/messages", conversationId)
						.header("X-Api-Key", TEST_API_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].role").value("USER"))
				.andExpect(jsonPath("$[1].role").value("ASSISTANT"));
	}

	@Test
	void sameIdempotencyKey_returnsSameAssistantMessageId() throws Exception {
		String idempotencyKey = UUID.randomUUID().toString();
		MvcResult first = mockMvc.perform(post("/api/conversations")
						.header("X-Api-Key", TEST_API_KEY)
						.header("Idempotency-Key", idempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isCreated())
				.andReturn();

		String assistantMessageId = readJsonString(first, "$.assistantMessageId");

		MvcResult second = mockMvc.perform(post("/api/conversations")
						.header("X-Api-Key", TEST_API_KEY)
						.header("Idempotency-Key", idempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"hello\"}"))
				.andExpect(status().isCreated())
				.andReturn();

		assertThat(readJsonString(second, "$.assistantMessageId")).isEqualTo(assistantMessageId);
	}

	@Test
	void historyForUnknownConversation_isNotFound() throws Exception {
		mockMvc.perform(get("/api/conversations/{cid}/messages", UUID.randomUUID())
						.header("X-Api-Key", TEST_API_KEY))
				.andExpect(status().isNotFound());
	}

	@Test
	void appendMessage_returnsAssistantReplyBasedOnRequestMessage() throws Exception {
		String createIdempotencyKey = UUID.randomUUID().toString();
		MvcResult created =
				mockMvc.perform(post("/api/conversations")
								.header("X-Api-Key", TEST_API_KEY)
								.header("Idempotency-Key", createIdempotencyKey)
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"message\":\"Hi\"}"))
						.andExpect(status().isCreated())
						.andExpect(jsonPath("$.conversationId").exists())
						.andReturn();

		String conversationId = readJsonString(created, "$.conversationId");

		String appendIdempotencyKey = UUID.randomUUID().toString();
		mockMvc.perform(post("/api/conversations/{cid}/messages", conversationId)
						.header("X-Api-Key", TEST_API_KEY)
						.header("Idempotency-Key", appendIdempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"2nd message\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
				.andExpect(jsonPath("$.assistantMessage").value("Mock assistant reply to: 2nd message"));
	}

	private static String readJsonString(MvcResult result, String jsonPath) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), jsonPath).toString();
	}
}
