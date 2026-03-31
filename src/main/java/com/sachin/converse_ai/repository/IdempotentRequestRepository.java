package com.sachin.converse_ai.repository;

import com.sachin.converse_ai.domain.IdempotentRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, UUID> {
	Optional<IdempotentRequest> findByUser_IdAndIdempotencyKey(UUID userId, String idempotencyKey);
}

