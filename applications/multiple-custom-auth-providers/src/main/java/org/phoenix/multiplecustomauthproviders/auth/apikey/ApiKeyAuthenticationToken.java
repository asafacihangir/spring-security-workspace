package org.phoenix.multiplecustomauthproviders.auth.apikey;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;


public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String apiKey;

    public ApiKeyAuthenticationToken(String apiKey) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = null;
        this.apiKey = apiKey;
        setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.apiKey = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
