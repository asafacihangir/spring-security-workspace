package org.phoenix.multiplecustomauthproviders.auth.apikey;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationProvider.class);

    private static final String API_KEY = "secret-api-key";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String presented = (String) authentication.getCredentials();
        if (!API_KEY.equals(presented)) {
            throw new BadCredentialsException("Geçersiz API key");
        }
        log.info("apiKeyProvider doğruladı (X-API-KEY)");
        return new ApiKeyAuthenticationToken("api-key-client", List.of(new SimpleGrantedAuthority("ROLE_API")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
