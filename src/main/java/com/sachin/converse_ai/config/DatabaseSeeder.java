package com.sachin.converse_ai.config;

import com.sachin.converse_ai.dao.ApiKey;
import com.sachin.converse_ai.dao.User;
import com.sachin.converse_ai.repository.ApiKeyRepository;
import com.sachin.converse_ai.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseSeeder {

	static final UUID SEEDED_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	static final UUID SEEDED_API_KEY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	// BCrypt hash for plaintext "test-api-key-001"
	static final String SEEDED_API_KEY_BCRYPT_HASH =
			"$2b$10$3.KXYmPLasY./slG6T4oKOq20WorvFbb3hCm9vsk8TFQpn50IC8K6";

	@Bean
	ApplicationRunner seedDatabase(UserRepository userRepository, ApiKeyRepository apiKeyRepository) {
		return args -> {
			if (userRepository.existsById(SEEDED_USER_ID)) {
				return;
			}

			Instant now = Instant.now();
			User user = new User(SEEDED_USER_ID, now);
			userRepository.save(user);

			ApiKey apiKey = new ApiKey(SEEDED_API_KEY_ID, user, SEEDED_API_KEY_BCRYPT_HASH, now, null);
			apiKeyRepository.save(apiKey);
		};
	}
}

