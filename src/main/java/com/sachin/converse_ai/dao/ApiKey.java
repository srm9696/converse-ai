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
@Table(name = "api_key")
public class ApiKey {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "user_id",
			nullable = false,
			updatable = false,
			foreignKey = @ForeignKey(name = "fk_api_key_user"))
	private User user;

	@Column(name = "api_key_hash", nullable = false, updatable = false, length = 100)
	private String apiKeyHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected ApiKey() {}

	public ApiKey(UUID id, User user, String apiKeyHash, Instant createdAt, Instant revokedAt) {
		this.id = id;
		this.user = user;
		this.apiKeyHash = apiKeyHash;
		this.createdAt = createdAt;
		this.revokedAt = revokedAt;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getApiKeyHash() {
		return apiKeyHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}
}
