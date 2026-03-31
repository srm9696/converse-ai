package com.sachin.converse_ai.repository;

import com.sachin.converse_ai.dao.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {
	List<Message> findAllByConversation_IdOrderByCreatedAtAsc(UUID conversationId);
}

