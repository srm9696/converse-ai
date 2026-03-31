package com.sachin.converse_ai.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "idempotent_request",
		uniqueConstraints = {
			@UniqueConstraint(
					name = "uq_idempotent_request_user_idempotency_key",
					columnNames = {"user_id", "idempotency_key"})
		},
		indexes = {
			@Index(name = "idx_idempotent_request_user_created_at", columnList = "user_id,created_at")
		})
public class IdempotentRequest {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "user_id",
			nullable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_idempotent_request_user"))
	private User user;

	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "conversation_id", nullable = false, updatable = false)
	private UUID conversationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private IdempotencyStatus status;

	@Column(name = "assistant_message_id")
	private UUID assistantMessageId;

	@Column(name = "assistant_message", length = 8000)
	private String assistantMessage;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected IdempotentRequest() {}

	public IdempotentRequest(
			UUID id,
			User user,
			String idempotencyKey,
			UUID conversationId,
			IdempotencyStatus status,
			UUID assistantMessageId,
			String assistantMessage,
			String errorCode,
			Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.user = user;
		this.idempotencyKey = idempotencyKey;
		this.conversationId = conversationId;
		this.status = status;
		this.assistantMessageId = assistantMessageId;
		this.assistantMessage = assistantMessage;
		this.errorCode = errorCode;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public IdempotencyStatus getStatus() {
		return status;
	}

	public UUID getAssistantMessageId() {
		return assistantMessageId;
	}

	public String getAssistantMessage() {
		return assistantMessage;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
