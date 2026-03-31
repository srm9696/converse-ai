package com.sachin.converse_ai.domain;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "message",
		indexes = {
			@Index(name = "idx_message_conversation_created_at", columnList = "conversation_id,created_at")
		})
public class Message {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "conversation_id",
			nullable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_message_conversation"))
	private Conversation conversation;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, updatable = false, length = 20)
	private MessageRole role;

	@Column(name = "content", nullable = false, updatable = false, length = 8000)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Message() {}

	public Message(UUID id, Conversation conversation, MessageRole role, String content, Instant createdAt) {
		this.id = id;
		this.conversation = conversation;
		this.role = role;
		this.content = content;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public Conversation getConversation() {
		return conversation;
	}

	public MessageRole getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

