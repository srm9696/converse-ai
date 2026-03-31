package com.sachin.converse_ai.repository;

import com.sachin.converse_ai.dao.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
	@Query("select k from ApiKey k join fetch k.user u where k.revokedAt is null and u.id = :userId")
	Optional<ApiKey> findActiveKeyByUserId(@Param("userId") UUID userId);

	@Query("select k from ApiKey k join fetch k.user u where k.revokedAt is null")
	List<ApiKey> findAllActiveWithUser();
}

