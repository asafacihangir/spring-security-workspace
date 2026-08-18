package org.phoenix.springsecurity.repository;

import java.util.Date;
import java.util.List;

import org.phoenix.springsecurity.domain.PersistentLogin;

import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

public class JpaPersistentTokenRepository implements PersistentTokenRepository {

	private final RememberMeTokenRepository rememberMeTokenRepository;

	public JpaPersistentTokenRepository(RememberMeTokenRepository rememberMeTokenRepository) {
		this.rememberMeTokenRepository = rememberMeTokenRepository;
	}

	@Override
	public void createNewToken(PersistentRememberMeToken token) {
		this.rememberMeTokenRepository.save(new PersistentLogin(token));
	}

	@Override
	public void updateToken(String series, String tokenValue, Date lastUsed) {
		PersistentLogin token = this.rememberMeTokenRepository.findBySeries(series);
		if (token != null) {
			token.setToken(tokenValue);
			token.setLastUsed(lastUsed);
			this.rememberMeTokenRepository.save(token);
		}
	}

	@Override
	public PersistentRememberMeToken getTokenForSeries(String seriesId) {
		PersistentLogin token = this.rememberMeTokenRepository.findBySeries(seriesId);
		if (token == null) {
			return null;
		}
		return new PersistentRememberMeToken(token.getUsername(),
				token.getSeries(),
				token.getToken(),
				token.getLastUsed());
	}

	@Override
	public void removeUserTokens(String username) {
		List<PersistentLogin> tokens = this.rememberMeTokenRepository.findByUsername(username);
		this.rememberMeTokenRepository.deleteAll(tokens);
	}

}
