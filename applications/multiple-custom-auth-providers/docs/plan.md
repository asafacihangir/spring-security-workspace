# Plan: Multiple Custom Authentication Providers Demo

## Context

`docs/spec.md` bir öğrenme/demo hedefi tanımlıyor: tek korumalı endpoint'te iki
`AuthenticationProvider`'ın (API key + dao/in-memory) bir arada çalışması ve
`ProviderManager`'ın `supports()` ile token tipine göre doğru provider'ı seçmesi.
İskelet hazır (Spring Boot 4.1.0, Spring Security 7, Java 21, security + webmvc
bağımlılıkları mevcut); sadece spec'teki bileşenler eklenecek. Odak
**authentication**; yetkilendirme kapsam dışı.

Paket kökü: `org.phoenix.multiplecustomauthproviders`

## Eklenecek dosyalar

Tümü `src/main/java/org/phoenix/multiplecustomauthproviders/` altına (tek paket,
en az dosya):

1. **`ApiKeyAuthenticationToken`** — `AbstractAuthenticationToken` extend eder.
   - Kimliği doğrulanmamış hali: ctor(String apiKey), `setAuthenticated(false)`.
   - Doğrulanmış hali: ctor(principal, authorities), `setAuthenticated(true)`.
   - `getCredentials()` → apiKey, `getPrincipal()` → principal.
   - `AbstractAuthenticationToken` kullanmak `Authentication`'ı elle implement
     etmekten kısa (ladder rung 4: hazır base class).

2. **`ApiKeyAuthenticationProvider implements AuthenticationProvider`**
   - `supports(Class<?>)` → `ApiKeyAuthenticationToken.isAssignableFrom(...)`.
   - `authenticate()` → sabit key ile karşılaştır; eşleşirse `log.info` + doğrulanmış
     token döner, değilse `BadCredentialsException`.
   - Sabit key burada `private static final String API_KEY = "..."` constant.

3. **`ApiKeyAuthFilter extends OncePerRequestFilter`**
   - `X-API-KEY` header yoksa → `filterChain.doFilter` ile geç (Basic Auth yoluna izin ver).
   - Varsa → `ApiKeyAuthenticationToken` (unauthenticated) üret, `AuthenticationManager.authenticate()`
     çağır, başarılıysa `SecurityContextHolder`'a koy. Hata yönetimi: `AuthenticationException`
     yakalanır, context temizlenir → zincirin sonunda 401 üretilir (sessiz yutma yok).
   - `AuthenticationManager`'ı ctor ile alır (SecurityConfig enjekte eder).

4. **`SecurityConfig`** (`@Configuration @EnableWebSecurity`)
   - `InMemoryUserDetailsManager` bean'i: tek sabit kullanıcı (örn. `user`/`password`, `{noop}`).
   - `DaoAuthenticationProvider` — Spring Security 7'de `new DaoAuthenticationProvider(userDetailsService)`
     ctor'u kullanılır (no-arg + setter deprecated).
   - `AuthenticationManager` bean'i = `new ProviderManager(apiKeyProvider, daoAuthenticationProvider)`.
   - `SecurityFilterChain` bean'i:
     - `authorizeHttpRequests(a -> a.anyRequest().authenticated())`
     - `httpBasic(Customizer.withDefaults())`
     - `addFilterBefore(apiKeyAuthFilter, BasicAuthenticationFilter.class)`
     - `csrf(csrf -> csrf.disable())` — stateless API, curl ile test (ponytail: demo, CSRF kapsam dışı).

5. **`WhoAmIController`** — `@RestController`, `GET /api/whoami` → `ResponseEntity.ok(...)`
   sade `200`. (İstenirse `Authentication`'ı parametre alıp principal adını döndürür;
   spec "sade 200" diyor — düz string yeterli.)

6. **`README.md`** (proje kökü) — kurgu anlatımı + 3 `curl` senaryosu.

## Doğrulama

Spec'teki 3 senaryo (`./mvnw spring-boot:run` sonrası):

1. `curl -u user:password localhost:8080/api/whoami` → `200`, log'da dao provider satırı
2. `curl -H "X-API-KEY: <key>" localhost:8080/api/whoami` → `200`, log'da apiKey satırı
3. `curl localhost:8080/api/whoami` (kimlik yok) / yanlış key → `401`

**Otomatik check** (ponytail: dallanma içeren mantık bir runnable check bırakır):
Mevcut `MultipleCustomAuthProvidersApplicationTests` boş `contextLoads`'ı genişletmek
yerine, `@SpringBootTest @AutoConfigureMockMvc` ile küçük bir test sınıfı — yukarıdaki
3 senaryoyu MockMvc ile assert eder (httpBasic + header + anonim → 200/200/401).
Test bağımlılıkları (`security-test`, `webmvc-test`) zaten pom'da var.

## Atlananlar (spec YAGNI ile uyumlu)

3. provider (JWT), rol/yetki, DB/gerçek key deposu, kullanıcı yönetimi UI → yok.
İhtiyaç olursa eklenir.
