package org.phoenix.rememberme;

import jakarta.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Faz 7 test support: mirrors {@code AbstractRememberMeServices#decodeCookie}
 * exactly, for the several Faz 7 test classes that need to pull the real
 * series/token pair out of a {@code PersistentTokenBasedRememberMeServices}
 * cookie value.
 *
 * <p>The cookie value is <em>not</em> plainly {@code base64("series:token")}:
 * {@code AbstractRememberMeServices.encodeCookie} URL-encodes each token
 * individually before joining them with {@code ":"} and base64-wrapping the
 * result (verified against that method's source - both {@code series} and
 * {@code token} are themselves base64 text containing {@code /} and
 * {@code =}, which is exactly what {@code URLEncoder.encode} exists to
 * protect the {@code ":"} delimiter from colliding with). Skipping the
 * per-token {@link URLDecoder#decode} step here would silently leave
 * {@code %2F}/{@code %3D} sequences in the "decoded" series/token instead of
 * {@code /}/{@code =} - a real bug this class's callers hit once (see
 * task-7-report.md) before this helper existed.
 */
final class PersistentCookieCodec {

    private PersistentCookieCodec() {
    }

    static String[] seriesAndToken(Cookie rememberMeCookie) {
        return seriesAndToken(rememberMeCookie.getValue());
    }

    static String[] seriesAndToken(String rawCookieValue) {
        StringBuilder padded = new StringBuilder(rawCookieValue);
        while (padded.length() % 4 != 0) {
            padded.append("=");
        }
        String joined = new String(Base64.getDecoder().decode(padded.toString()), StandardCharsets.UTF_8);
        String[] rawTokens = joined.split(":");
        String[] decoded = new String[rawTokens.length];
        for (int i = 0; i < rawTokens.length; i++) {
            decoded[i] = urlDecode(rawTokens[i]);
        }
        return decoded;
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
