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
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

	private final IdempotentRequestRepository idempotentRequestRepository;

	public IdempotencyService(IdempotentRequestRepository idempotentRequestRepository) {
		this.idempotentRequestRepository = idempotentRequestRepository;
	}

	public sealed interface Gate permits Proceed, Completed, InProgress, Failed {}

	public enum Proceed implements Gate {
		INSTANCE
	}

	public record Completed(ConversationMessageResponse response) implements Gate {}

	public enum InProgress implements Gate {
		INSTANCE
	}

	public enum Failed implements Gate {
		INSTANCE
	}

	@Transactional
	public Gate beginOrResolve(UUID userId, User user, String idempotencyKey, UUID conversationId) {
		String normalizedKey = idempotencyKey.strip();
		if (normalizedKey.isEmpty()) {
			throw new IdempotencyConflictException("Idempotency-Key must not be blank");
		}

		while (true) {
			Optional<IdempotentRequest> existing =
					idempotentRequestRepository.findByUser_IdAndIdempotencyKey(userId, normalizedKey);
			if (existing.isPresent()) {
				return mapExisting(existing.get(), conversationId);
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
				return Proceed.INSTANCE;
			} catch (DataIntegrityViolationException e) {
				// Concurrent claim on the same (userId, idempotencyKey); retry resolution.
			}
		}
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

	@Transactional
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

	@Transactional
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
