package com.sachin.converse_ai.repository;

import com.sachin.converse_ai.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {}

