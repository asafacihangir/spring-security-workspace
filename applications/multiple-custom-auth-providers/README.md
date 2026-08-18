# Multiple Custom Authentication Providers Demo

Bu örnekte tek bir korumalı endpoint var:

```http
GET /api/whoami
```

Bu endpoint'e istek attığımızda sisteme iki farklı şekilde kim olduğumuzu
söyleyebiliriz:

1. Kullanıcı adı ve şifre ile, yani Basic Auth
2. API key ile, yani `X-API-KEY` header'ı göndererek

Yani aynı kapıya geliyoruz, ama elimizde iki farklı kimlik kartı olabilir.

## Akışın Hikayesi

İstek önce Spring Security filter zincirine gelir.

Eğer istekte şu header varsa:

```http
X-API-KEY: secret-api-key
```

bu isteği bizim yazdığımız `ApiKeyAuthFilter` yakalar. Header'daki değeri alır ve
bir `ApiKeyAuthenticationToken` oluşturur.

Eğer istek Basic Auth ile geldiyse:

```bash
curl -u user:password localhost:8080/api/whoami
```

bu kez Spring'in kendi `BasicAuthenticationFilter`'ı devreye girer ve bir
`UsernamePasswordAuthenticationToken` oluşturur.

Bundan sonrası `ProviderManager`'ın işidir. `ProviderManager`, elindeki
authentication token'a bakar ve şu soruyu sorar:

> Bu token'ı hangi provider doğrulayabilir?

Eğer token `ApiKeyAuthenticationToken` ise:

```java
apiKeyProvider.supports(ApiKeyAuthenticationToken.class)
```

çalışır ve API key kontrolü bizim custom provider içinde yapılır.

Eğer token `UsernamePasswordAuthenticationToken` ise:

```java
daoAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class)
```

çalışır ve kullanıcı adı / şifre kontrolü `InMemoryUserDetailsManager`
üzerinden yapılır.

Özetle aynı endpoint korunur, ama iki farklı doğrulama yolu desteklenir.

## Sabit Kimlikler

- **Basic Auth:** `user` / `password`
- **API key:** header `X-API-KEY: secret-api-key`

## Deneme Senaryoları

Basic Auth ile doğru giriş:

```bash
curl -u user:password localhost:8080/api/whoami
```

`daoAuthenticationProvider` doğrular → `200` ve gövdede `authenticated as: user`.

API key ile doğru giriş:

```bash
curl -H "X-API-KEY: secret-api-key" localhost:8080/api/whoami
```

`ApiKeyAuthenticationProvider` doğrular → `200` ve gövdede
`authenticated as: api-key-client`. Log'da şu satır görülür:

```text
apiKeyProvider doğruladı (X-API-KEY)
```

Kimlik bilgisi olmadan veya yanlış API key ile:

```bash
curl -i localhost:8080/api/whoami
curl -i -H "X-API-KEY: wrong" localhost:8080/api/whoami
```

Spring Security isteği reddeder ve `401 Unauthorized` döner.

## Çalıştırma

```bash
./mvnw spring-boot:run
```

## Test

```bash
./mvnw test
```

## Kaynaklar

- [Multiple Security Filter Chain in Spring Security with Providers](https://medium.com/@siratsemih/multiple-security-filter-chain-in-spring-security-with-providers-af62256a0a16)
- [Spring Security Part V: Implementing Multiple Authentication Providers](https://java-jedi.medium.com/spring-security-part-v-implementing-multiple-authentication-providers-f80a459a5ec3)
