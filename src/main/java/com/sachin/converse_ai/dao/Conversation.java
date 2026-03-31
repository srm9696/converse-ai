package com.sachin.converse_ai.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation")
public class Conversation {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "user_id",
			nullable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_conversation_user"))
	private User user;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Conversation() {}

	public Conversation(UUID id, User user, Instant createdAt) {
		this.id = id;
		this.user = user;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
