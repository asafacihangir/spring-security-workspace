# Spring Security Custom Authentication Providers Demo — Tasarım

**Tarih:** 2026-06-17
**Konum:** `security/spring-security-custom-providers/`
**Yığın:** Spring Boot 4.0.0, Spring Security 7, Java 21, Maven

## Amaç

Tek bir korumalı endpoint'te iki `AuthenticationProvider`'ın bir arada çalışmasını ve
`ProviderManager`'ın `supports()` ile doğru provider'ı seçmesini öğrenmek/göstermek.
Odak **authentication**'da; yetkilendirme kapsam dışı.

## Kimlik Akışı

Tek endpoint, iki kimlik yöntemi aynı anda kabul edilir. `ProviderManager` token tipine
göre uygun provider'ı seçer.

```
İstek → SecurityFilterChain
   ├─ X-API-KEY header varsa → ApiKeyAuthFilter → ApiKeyAuthenticationToken
   └─ Authorization: Basic   → (Spring hazır filtresi) → UsernamePasswordAuthenticationToken
        ↓
   ProviderManager
     ├─ apiKeyProvider.supports(ApiKeyAuthenticationToken)   → sabit key kontrolü
     └─ daoAuthenticationProvider.supports(UsernamePassword) → InMemoryUserDetailsManager
```

## Bileşenler

| Dosya | Görevi |
|---|---|
| `ApiKeyAuthenticationToken` | Özel `Authentication` implementasyonu (API key tarafının token tipi) |
| `ApiKeyAuthFilter` | `X-API-KEY` header'ını okur, kimliği doğrulanmamış token üretir ve `AuthenticationManager`'a verir; başarılıysa sonucu `SecurityContext`'e koyar |
| `ApiKeyAuthenticationProvider` | `supports(ApiKeyAuthenticationToken)` + sabit key doğrulaması; doğrulama anında `log.info` |
| `SecurityConfig` | İki provider'ı `ProviderManager`'a kaydeder; `DaoAuthenticationProvider` + `InMemoryUserDetailsManager`; `ApiKeyAuthFilter`'ı zincire ekler; tüm istekler `authenticated()` ister |
| `WhoAmIController` | `GET /api/whoami` → sade `200` cevabı döner |
| `README.md` | Kurgu anlatımı + `curl` örnekleri |

## Kararlar

- **API key kaynağı:** Tek sabit key (en minimal doğru/yanlış kontrolü). DB/key store yok.
- **Kullanıcı kaynağı (dao):** `InMemoryUserDetailsManager` ile sabit bir kullanıcı.
- **Gözlemlenebilirlik:** Endpoint sade `200` döner. Hangi provider'ın doğruladığı her
  provider'ın `authenticate()` içindeki `log.info` satırından izlenir.

## Kapsam Dışı (YAGNI)

- 3. provider (örn. JWT) → yazılmaz.
- Rol/yetki bazlı erişim → endpoint sadece `authenticated()` ister.
- Veritabanı, gerçek API key deposu, kullanıcı yönetimi UI'ı → yok.

## Doğrulama

`curl` ile üç senaryo:

1. Doğru Basic Auth → `200` + log'da dao provider satırı
2. Doğru `X-API-KEY` → `200` + log'da apiKey provider satırı
3. İkisi de yanlış / kimlik yok → `401`