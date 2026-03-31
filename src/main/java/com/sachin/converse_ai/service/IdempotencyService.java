package com.sachin.converse_ai.service;

import com.sachin.converse_ai.dao.IdempotencyStatus;
import com.sachin.converse_ai.dao.IdempotentRequest;
import com.sachin.converse_ai.dao.User;
import com.sachin.converse_ai.dto.ConversationMessageResponse;
import com.sachin.converse_ai.exception.IdempotencyConflictException;
import com.sachin.converse_ai.repository.IdempotentRequestRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

	private final IdempotentRequestRepository idempotentRequestRepository;

	public IdempotencyService(IdempotentRequestRepository idempotentRequestRepository) {
		this.idempotentRequestRepository = idempotentRequestRepository;
	}

	public sealed interface Gate permits Proceed, Completed, InProgress, Failed {}

	public record Proceed(UUID conversationId) implements Gate {}

	public record Completed(ConversationMessageResponse response) implements Gate {}

	public enum InProgress implements Gate {
		INSTANCE
	}

	public enum Failed implements Gate {
		INSTANCE
	}

	/**
	 * Create flow: if a record already exists for this key, it is evaluated against the conversation id stored
	 * for that key (a fresh random UUID must not be generated first, otherwise retries look like a “different
	 * conversation” collision).
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Gate prepareCreateConversation(UUID userId, User user, String idempotencyKey) {
		String normalizedKey = normalizeKey(idempotencyKey);
		Optional<IdempotentRequest> existing =
				idempotentRequestRepository.findByUser_IdAndIdempotencyKey(userId, normalizedKey);
		if (existing.isPresent()) {
			return mapExisting(existing.get(), existing.get().getConversationId());
		}
		return insertNewCreateClaimLoop(userId, user, normalizedKey);
	}

	/**
	 * Append flow: the path conversation id is part of the idempotency scope and must match any existing row.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Gate prepareAppendMessage(UUID userId, User user, String idempotencyKey, UUID conversationId) {
		String normalizedKey = normalizeKey(idempotencyKey);
		Optional<IdempotentRequest> existing =
				idempotentRequestRepository.findByUser_IdAndIdempotencyKey(userId, normalizedKey);
		if (existing.isPresent()) {
			return mapExisting(existing.get(), conversationId);
		}
		return insertNewAppendClaimLoop(userId, user, normalizedKey, conversationId);
	}

	private Gate insertNewCreateClaimLoop(UUID userId, User user, String normalizedKey) {
		while (true) {
			Optional<IdempotentRequest> racing =
					idempotentRequestRepository.findByUser_IdAndIdempotencyKey(userId, normalizedKey);
			if (racing.isPresent()) {
				return mapExisting(racing.get(), racing.get().getConversationId());
			}

			UUID conversationId = UUID.randomUUID();
			Instant now = Instant.now();
			IdempotentRequest row = new IdempotentRequest(
					UUID.randomUUID(),
					user,
					normalizedKey,
					conversationId,
					IdempotencyStatus.IN_PROGRESS,
					null,
					null,
					null,
					null,
					now,
					now);
			try {
				idempotentRequestRepository.saveAndFlush(row);
				return new Proceed(conversationId);
			} catch (DataIntegrityViolationException e) {
				// Concurrent claim on the same (userId, idempotencyKey); retry resolution.
			}
		}
	}

	private Gate insertNewAppendClaimLoop(UUID userId, User user, String normalizedKey, UUID conversationId) {
		while (true) {
			Optional<IdempotentRequest> racing =
					idempotentRequestRepository.findByUser_IdAndIdempotencyKey(userId, normalizedKey);
			if (racing.isPresent()) {
				return mapExisting(racing.get(), conversationId);
			}

			Instant now = Instant.now();
			IdempotentRequest row = new IdempotentRequest(
					UUID.randomUUID(),
					user,
					normalizedKey,
					conversationId,
					IdempotencyStatus.IN_PROGRESS,
					null,
					null,
					null,
					null,
					now,
					now);
			try {
				idempotentRequestRepository.saveAndFlush(row);
				return new Proceed(conversationId);
			} catch (DataIntegrityViolationException e) {
				// Concurrent claim on the same (userId, idempotencyKey); retry resolution.
			}
		}
	}

	private String normalizeKey(String idempotencyKey) {
		String normalizedKey = idempotencyKey.strip();
		if (normalizedKey.isEmpty()) {
			throw new IdempotencyConflictException("Idempotency-Key must not be blank");
		}
		return normalizedKey;
	}

	private Gate mapExisting(IdempotentRequest row, UUID expectedConversationId) {
		if (!row.getConversationId().equals(expectedConversationId)) {
			throw new IdempotencyConflictException("Idempotency-Key belongs to a different conversation");
		}

		return switch (row.getStatus()) {
			case COMPLETED -> {
				if (row.getAssistantMessageId() == null
						|| row.getAssistantMessage() == null
						|| row.getAssistantCreatedAt() == null) {
					throw new IdempotencyConflictException("Stored idempotent response is incomplete");
				}
				yield new Completed(new ConversationMessageResponse(
						row.getConversationId(),
						row.getAssistantMessageId(),
						row.getAssistantMessage(),
						row.getAssistantCreatedAt()));
			}
			case IN_PROGRESS -> InProgress.INSTANCE;
			case FAILED -> {
				// A FAILED idempotency key is not retried automatically.
				// The client must submit a new Idempotency-Key to retry.
				yield Failed.INSTANCE;
			}
		};
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void markCompleted(UUID userId, String idempotencyKey, ConversationMessageResponse response) {
		IdempotentRequest row = idempotentRequestRepository
				.findByUser_IdAndIdempotencyKey(userId, idempotencyKey.strip())
				.orElseThrow();
		row.setStatus(IdempotencyStatus.COMPLETED);
		row.setAssistantMessageId(response.assistantMessageId());
		row.setAssistantMessage(response.assistantMessage());
		row.setAssistantCreatedAt(response.createdAt());
		row.setErrorCode(null);
		row.setUpdatedAt(Instant.now());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(UUID userId, String idempotencyKey, String errorCode) {
		Optional<IdempotentRequest> row =
				idempotentRequestRepository.findByUser_IdAndIdempotencyKey(userId, idempotencyKey.strip());
		if (row.isEmpty()) {
			return;
		}
		IdempotentRequest entity = row.get();
		entity.setStatus(IdempotencyStatus.FAILED);
		entity.setErrorCode(errorCode);
		entity.setUpdatedAt(Instant.now());
	}
}
