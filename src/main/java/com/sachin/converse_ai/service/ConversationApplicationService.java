package com.sachin.converse_ai.service;

import com.sachin.converse_ai.client.LlmChatCommand;
import com.sachin.converse_ai.client.LlmClient;
import com.sachin.converse_ai.dao.Conversation;
import com.sachin.converse_ai.dao.Message;
import com.sachin.converse_ai.dao.MessageRole;
import com.sachin.converse_ai.dao.User;
import com.sachin.converse_ai.dto.ConversationHistoryItem;
import com.sachin.converse_ai.dto.ConversationMessageResponse;
import com.sachin.converse_ai.exception.IdempotencyConflictException;
import com.sachin.converse_ai.exception.IdempotencyFailedKeyException;
import com.sachin.converse_ai.exception.ResourceNotFoundException;
import com.sachin.converse_ai.repository.ConversationRepository;
import com.sachin.converse_ai.repository.MessageRepository;
import com.sachin.converse_ai.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationApplicationService {

	private final UserRepository userRepository;
	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final LlmClient llmClient;
	private final IdempotencyService idempotencyService;

	public ConversationApplicationService(
			UserRepository userRepository,
			ConversationRepository conversationRepository,
			MessageRepository messageRepository,
			LlmClient llmClient,
			IdempotencyService idempotencyService) {
		this.userRepository = userRepository;
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.llmClient = llmClient;
		this.idempotencyService = idempotencyService;
	}

	@Transactional
	public ConversationMessageResponse createConversationWithFirstMessage(
			UUID userId, String idempotencyKey, String messageText) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		UUID conversationId = UUID.randomUUID();
		Instant now = Instant.now();

		IdempotencyService.Gate gate = idempotencyService.beginOrResolve(userId, user, idempotencyKey, conversationId);
		if (gate instanceof IdempotencyService.Completed completed) {
			return completed.response();
		}
		if (gate instanceof IdempotencyService.InProgress) {
			throw new IdempotencyConflictException(
					"Duplicate request with the same Idempotency-Key is already being processed");
		}
		if (gate instanceof IdempotencyService.Failed) {
			throw new IdempotencyFailedKeyException(
					"This Idempotency-Key already failed. Submit a new Idempotency-Key to retry.");
		}

		try {
			Conversation conversation = new Conversation(conversationId, user, now);
			conversationRepository.save(conversation);

			Message assistant = appendUserMessageAndCompleteAssistant(conversation, messageText, now);
			ConversationMessageResponse response = new ConversationMessageResponse(
					conversation.getId(), assistant.getId(), assistant.getContent(), assistant.getCreatedAt());
			idempotencyService.markCompleted(userId, idempotencyKey, response);
			return response;
		} catch (RuntimeException ex) {
			idempotencyService.markFailed(userId, idempotencyKey, "PROCESSING_FAILED");
			throw ex;
		}
	}

	@Transactional
	public ConversationMessageResponse appendAssistantReply(
			UUID userId, UUID conversationId, String idempotencyKey, String messageText) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Conversation conversation = conversationRepository
				.findByIdAndUser_Id(conversationId, user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
		Instant now = Instant.now();

		IdempotencyService.Gate gate = idempotencyService.beginOrResolve(userId, user, idempotencyKey, conversation.getId());
		if (gate instanceof IdempotencyService.Completed completed) {
			return completed.response();
		}
		if (gate instanceof IdempotencyService.InProgress) {
			throw new IdempotencyConflictException(
					"Duplicate request with the same Idempotency-Key is already being processed");
		}
		if (gate instanceof IdempotencyService.Failed) {
			throw new IdempotencyFailedKeyException(
					"This Idempotency-Key already failed. Submit a new Idempotency-Key to retry.");
		}

		try {
			Message assistant = appendUserMessageAndCompleteAssistant(conversation, messageText, now);
			ConversationMessageResponse response = new ConversationMessageResponse(
					conversation.getId(), assistant.getId(), assistant.getContent(), assistant.getCreatedAt());
			idempotencyService.markCompleted(userId, idempotencyKey, response);
			return response;
		} catch (RuntimeException ex) {
			idempotencyService.markFailed(userId, idempotencyKey, "PROCESSING_FAILED");
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public List<ConversationHistoryItem> loadHistory(UUID userId, UUID conversationId) {
		Conversation conversation = conversationRepository
				.findByIdAndUser_Id(conversationId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
		return messageRepository.findAllByConversation_IdOrderByCreatedAtAsc(conversation.getId()).stream()
				.map(m -> new ConversationHistoryItem(m.getRole(), m.getContent(), m.getCreatedAt()))
				.toList();
	}

	private Message appendUserMessageAndCompleteAssistant(Conversation conversation, String messageText, Instant userMessageTime) {
		Message userMessage = new Message(UUID.randomUUID(), conversation, MessageRole.USER, messageText, userMessageTime);
		messageRepository.save(userMessage);

		var completion = llmClient.complete(new LlmChatCommand(conversation.getId(), messageText));
		Instant assistantTimestamp = Instant.now();
		Message assistantMessage =
				new Message(UUID.randomUUID(), conversation, MessageRole.ASSISTANT, completion.assistantMessage(), assistantTimestamp);
		messageRepository.save(assistantMessage);
		return assistantMessage;
	}
}
