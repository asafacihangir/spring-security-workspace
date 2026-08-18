package org.phoenix.springsecurity.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.phoenix.springsecurity.repository.RememberMeTokenRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class RememberMeCleanup {

	// Spring Security's default rememberMe token validity.
	private static final Duration TOKEN_VALIDITY = Duration.ofDays(14);

	private static final Logger logger = LoggerFactory.getLogger(RememberMeCleanup.class);

	private final RememberMeTokenRepository rememberMeTokenRepository;

	public RememberMeCleanup(RememberMeTokenRepository rememberMeTokenRepository) {
		this.rememberMeTokenRepository = rememberMeTokenRepository;
	}

	@Scheduled(fixedRate = 600_000)
	public void removeExpiredTokens() {
		Date cutoff = Date.from(Instant.now().minus(TOKEN_VALIDITY));
		int removed = rememberMeTokenRepository.deleteByLastUsedBefore(cutoff);
		if (removed > 0) {
			logger.info("Removed {} persistent logins last used before {}", removed, cutoff);
		}
	}

}
