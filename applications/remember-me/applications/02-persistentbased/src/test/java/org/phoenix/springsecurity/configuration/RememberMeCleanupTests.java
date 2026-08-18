package org.phoenix.springsecurity.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.phoenix.springsecurity.repository.RememberMeTokenRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RememberMeCleanupTests {

	@Mock
	private RememberMeTokenRepository rememberMeTokenRepository;

	@Test
	void deletesTokensLastUsedMoreThanFourteenDaysAgo() {
		Instant startedAt = Instant.now();

		new RememberMeCleanup(rememberMeTokenRepository).removeExpiredTokens();

		ArgumentCaptor<Date> cutoff = ArgumentCaptor.forClass(Date.class);
		verify(rememberMeTokenRepository).deleteByLastUsedBefore(cutoff.capture());
		assertThat(cutoff.getValue().toInstant()).isBetween(
				startedAt.minus(Duration.ofDays(14)),
				Instant.now().minus(Duration.ofDays(14)));
	}

}
