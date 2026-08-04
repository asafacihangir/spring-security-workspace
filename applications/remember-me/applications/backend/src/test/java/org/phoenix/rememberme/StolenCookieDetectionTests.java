package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Faz 7 (UC-012, Test Adimi 5): proves BR-017/018 end to end through the
 * real {@code PersistentTokenBasedRememberMeServices} auto-login path, the
 * same way {@link TokenRotationTests} proves BR-015/016 - driving actual
 * HTTP requests, not calling {@code PersistentTokenRepository} methods
 * directly. "Cookie theft" is simulated exactly the way UC-012's Test Adimi
 * 3 describes it manually: hang onto a copy of a remember-me cookie value
 * from before the token rotates, let the legitimate browser rotate it, then
 * replay the now-stale copy. No new attack machinery is built here (Faz 7's
 * brief is explicit that theft detection is Spring Security's existing
 * behavior, not something this phase builds) - this class only observes it.
 *
 * <p><b>Why a real embedded server ({@code webEnvironment = RANDOM_PORT} +
 * {@link TestRestTemplate}) instead of {@code MockMvc}, unlike every other
 * test class in this suite:</b> {@code PersistentTokenBasedRememberMeServices}
 * responds to a stale-token replay by throwing {@code CookieTheftException}
 * from inside {@code RememberMeAuthenticationFilter.doFilter} - a point in
 * Spring Security's default filter order (see
 * {@code FilterOrderRegistration}) that runs <em>before</em>
 * {@code ExceptionTranslationFilter}, so that filter never gets a chance to
 * translate it into this app's {@code HttpStatusEntryPoint(UNAUTHORIZED)}
 * the way it does for every other {@code AuthenticationException} in this
 * codebase. Under {@code MockMvc} - which has no embedded servlet
 * container behind it - that exception simply propagates out of
 * {@code mockMvc.perform(...)} uncaught (verified empirically while writing
 * this class). Against a real running instance (verified both by this
 * class and by hand with {@code curl} - see task-7-report.md), it still
 * comes back as a clean {@code 401} with the remember-me cookie cancelled:
 * Spring Boot's embedded Tomcat catches the escaping exception, forwards
 * the request to {@code /error} per its default error-page mapping, and
 * {@code /error} itself is not on this app's {@code permitAll} list - so
 * that forwarded, still-unauthenticated request falls through to
 * {@code anyRequest().authenticated()} and gets this app's own
 * {@code HttpStatusEntryPoint(UNAUTHORIZED)} treatment <em>indirectly</em>,
 * one request later, rather than the original request itself being
 * translated. The end-to-end observable behavior UC-012 describes (a clean
 * rejection, cookie cancelled) holds either way - but only a real HTTP
 * round trip proves that; MockMvc would report a false negative here.
 *
 * <p><b>The BR-018 scope finding</b> (see
 * {@link #findingSpringDeletesEveryDeviceSeriesForTheUserNotJustTheStolenOne()}):
 * {@code PersistentTokenBasedRememberMeServices.processAutoLoginCookie}
 * responds to a token mismatch by calling
 * {@code tokenRepository.removeUserTokens(token.getUsername())}, and
 * {@code JdbcTokenRepositoryImpl.removeUserTokens} runs
 * {@code delete from persistent_logins where username = ?} - scoped to the
 * <em>username</em>, not the series. BR-018's Turkish wording ("yalnizca
 * ilgili istek degil, o series'e bagli tum hatirlanma kayitlari iptal
 * edilir") reads as scoping the cancellation to the one compromised
 * series. Spring's built-in behavior is broader than that literal wording:
 * it cancels every persistent login the user has, on every device, the
 * moment any one of that user's cookies is replayed stale - not just the
 * series the stale cookie belonged to. For this lab's single-device manual
 * walkthrough (Test Adimi 1-4) the two behaviors are indistinguishable
 * (there is only ever one series to begin with), but it is worth stating
 * precisely rather than assuming BR-018's literal scope holds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.remember-me.strategy=persistent")
class StolenCookieDetectionTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private String loginWithRememberMeAndCaptureCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", DemoUserSeeder.DEMO_USERNAME);
        form.add("password", DemoUserSeeder.DEMO_PASSWORD);
        // Faz 9 (UC-015): "keep-me" is this app's configured parameter name
        // (app.remember-me.parameter-name), not Spring's default.
        form.add("keep-me", "true");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/login", new HttpEntity<>(form, headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String cookieValue = extractRememberMeCookieValue(response);
        assertThat(cookieValue).as("login with keep-me=true must set a notes-rm cookie").isNotNull();
        return cookieValue;
    }

    /** Cookie-only request - no session - the same auto-login trigger UC-011/012 describe. */
    private ResponseEntity<String> requestMeWithRememberMeCookie(String cookieValue) {
        HttpHeaders headers = new HttpHeaders();
        // Faz 9 (UC-015): "notes-rm" is this app's configured cookie name
        // (app.remember-me.cookie-name), not Spring's default.
        headers.add(HttpHeaders.COOKIE, "notes-rm=" + cookieValue);
        return restTemplate.exchange("/api/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private static String extractRememberMeCookieValue(ResponseEntity<?> response) {
        List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookieHeaders == null) {
            return null;
        }
        for (String header : setCookieHeaders) {
            if (header.startsWith("notes-rm=")) {
                String pair = header.split(";", 2)[0];
                return pair.substring("notes-rm=".length());
            }
        }
        return null;
    }

    private static String series(String cookieValue) {
        return PersistentCookieCodec.seriesAndToken(cookieValue)[0];
    }

    @Test
    void br017_replayingAStaleTokenAfterRotationIsAlwaysRejected() {
        String staleCookieCopy = loginWithRememberMeAndCaptureCookie();

        // UC-012 main scenario step 2: the legitimate browser auto-logs in
        // first (cookie-only request), rotating the token and making the
        // earlier copy stale (BR-015).
        assertThat(requestMeWithRememberMeCookie(staleCookieCopy).getStatusCode()).isEqualTo(HttpStatus.OK);

        // UC-012 step 3-4/BR-017: replaying the now-stale copy - valid
        // series, wrong token - is never accepted, full stop.
        assertThat(requestMeWithRememberMeCookie(staleCookieCopy).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void br018_theftWipesTheStolenSeriesAndTheLegitimateBrowserIsAlsoForcedToReLogin() {
        String staleCookieCopy = loginWithRememberMeAndCaptureCookie();
        String stolenSeries = series(staleCookieCopy);

        ResponseEntity<String> legitimateRotation = requestMeWithRememberMeCookie(staleCookieCopy);
        assertThat(legitimateRotation.getStatusCode()).isEqualTo(HttpStatus.OK);
        String legitimateRotatedCookie = extractRememberMeCookieValue(legitimateRotation);
        assertThat(legitimateRotatedCookie).isNotNull();

        // The attacker's replay: rejected (BR-017), and triggers cancellation.
        assertThat(requestMeWithRememberMeCookie(staleCookieCopy).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // BR-018 success postcondition: the stolen series' row is gone entirely.
        Long remainingForStolenSeries = jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where series = ?", Long.class, stolenSeries);
        assertThat(remainingForStolenSeries).isZero();

        // UC-012 main scenario step 7 / BR-018: the legitimate browser's own
        // cookie - freshly rotated moments ago, never itself replayed or
        // stale - is now ALSO rejected, because its series record no longer
        // exists at all. The user has to log in again "everywhere", not just
        // on the compromised device.
        assertThat(requestMeWithRememberMeCookie(legitimateRotatedCookie).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void findingSpringDeletesEveryDeviceSeriesForTheUserNotJustTheStolenOne() {
        // Simulates two devices for the same demo user: each remember-me
        // login creates a brand new series (onLoginSuccess calls
        // generateSeriesData() every time), so logging in twice gives two
        // independent, simultaneously-valid series/token rows for one user -
        // exactly what "logged in on phone and laptop" looks like server-side.
        String deviceA = loginWithRememberMeAndCaptureCookie();
        String deviceB = loginWithRememberMeAndCaptureCookie();
        String deviceASeries = series(deviceA);
        String deviceBSeries = series(deviceB);
        assertThat(deviceASeries).isNotEqualTo(deviceBSeries);

        List<Map<String, Object>> rowsBeforeTheft = jdbcTemplate.queryForList(
                "select series from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
        assertThat(rowsBeforeTheft).hasSize(2);

        // Rotate device A's token (so its original cookie value goes stale),
        // then replay the stale copy - theft is detected for device A's
        // series specifically. Device B is never touched by any of this.
        assertThat(requestMeWithRememberMeCookie(deviceA).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(requestMeWithRememberMeCookie(deviceA).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // BR-018's literal wording ("o series'e bagli tum hatirlanma
        // kayitlari") would predict device B's still-untouched, still-fresh
        // series survives. It does not: removeUserTokens(username) deletes
        // every row for the user, so device B's row is gone too.
        List<Map<String, Object>> rowsAfterTheft = jdbcTemplate.queryForList(
                "select series from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
        assertThat(rowsAfterTheft).isEmpty();

        // Concretely: device B's own cookie - never replayed, never stale,
        // completely uninvolved in the "theft" - is now also rejected. This
        // is the collateral-damage consequence of the user-wide (not
        // series-wide) deletion documented in this class's javadoc.
        assertThat(requestMeWithRememberMeCookie(deviceB).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

}
