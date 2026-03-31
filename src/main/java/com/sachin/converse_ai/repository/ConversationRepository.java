package com.sachin.converse_ai.repository;

import com.sachin.converse_ai.dao.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
	Optional<Conversation> findByIdAndUser_Id(UUID conversationId, UUID userId);
}

