package org.phoenix.springsecurity.repository;

import java.util.Date;
import java.util.List;

import org.phoenix.springsecurity.domain.PersistentLogin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaPersistentTokenRepositoryTests {

	private static final Date LAST_USED = new Date(1_700_000_000_000L);

	@Mock
	private RememberMeTokenRepository rememberMeTokenRepository;

	private JpaPersistentTokenRepository repository() {
		return new JpaPersistentTokenRepository(rememberMeTokenRepository);
	}

	@Test
	void createNewTokenMapsEveryField() {
		repository().createNewToken(
				new PersistentRememberMeToken("user1@example.com", "series-1", "token-1", LAST_USED));

		ArgumentCaptor<PersistentLogin> saved = ArgumentCaptor.forClass(PersistentLogin.class);
		verify(rememberMeTokenRepository).save(saved.capture());
		assertThat(saved.getValue().getUsername()).isEqualTo("user1@example.com");
		assertThat(saved.getValue().getSeries()).isEqualTo("series-1");
		assertThat(saved.getValue().getToken()).isEqualTo("token-1");
		assertThat(saved.getValue().getLastUsed()).isEqualTo(LAST_USED);
	}

	@Test
	void updateTokenRotatesTokenAndKeepsSeries() {
		PersistentLogin existing = new PersistentLogin(
				new PersistentRememberMeToken("user1@example.com", "series-1", "old-token", LAST_USED));
		given(rememberMeTokenRepository.findBySeries("series-1")).willReturn(existing);
		Date now = new Date(LAST_USED.getTime() + 60_000L);

		repository().updateToken("series-1", "new-token", now);

		verify(rememberMeTokenRepository).save(existing);
		assertThat(existing.getSeries()).isEqualTo("series-1");
		assertThat(existing.getToken()).isEqualTo("new-token");
		assertThat(existing.getLastUsed()).isEqualTo(now);
	}

	@Test
	void updateTokenIsNoOpForUnknownSeries() {
		given(rememberMeTokenRepository.findBySeries("nope")).willReturn(null);

		repository().updateToken("nope", "new-token", LAST_USED);

		verify(rememberMeTokenRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void getTokenForSeriesMapsBack() {
		given(rememberMeTokenRepository.findBySeries("series-1")).willReturn(new PersistentLogin(
				new PersistentRememberMeToken("user1@example.com", "series-1", "token-1", LAST_USED)));

		PersistentRememberMeToken token = repository().getTokenForSeries("series-1");

		assertThat(token.getUsername()).isEqualTo("user1@example.com");
		assertThat(token.getSeries()).isEqualTo("series-1");
		assertThat(token.getTokenValue()).isEqualTo("token-1");
		assertThat(token.getDate()).isEqualTo(LAST_USED);
	}

	@Test
	void getTokenForSeriesReturnsNullWhenMissing() {
		given(rememberMeTokenRepository.findBySeries("nope")).willReturn(null);

		assertThat(repository().getTokenForSeries("nope")).isNull();
	}

	@Test
	void removeUserTokensDeletesEverySeriesOfThatUser() {
		List<PersistentLogin> tokens = List.of(
				new PersistentLogin(
						new PersistentRememberMeToken("user1@example.com", "series-1", "token-1", LAST_USED)),
				new PersistentLogin(
						new PersistentRememberMeToken("user1@example.com", "series-2", "token-2", LAST_USED)));
		given(rememberMeTokenRepository.findByUsername("user1@example.com")).willReturn(tokens);

		repository().removeUserTokens("user1@example.com");

		verify(rememberMeTokenRepository).deleteAll(tokens);
	}

}
