# Remember-Me Lab

Spring Security'nin token-based ve persistent-token remember-me stratejilerini
uçtan uca deneyebileceğimiz bir demo uygulama. Kullanıcı adı/şifre ile giriş
yapar, notlarını yönetir, "Remember Me" ile oturumu hayatta tutar ve
Token Inspector üzerinden arka planda ne olduğunu izler.

Bu commit yalnızca **Faz 0 — Altyapı ve İskelet**'i içerir: kimlik doğrulama
mantığı henüz yok, sadece MySQL + backend + frontend'in birlikte ayağa
kalktığı boş bir iskelet var. Sonraki fazlar için bkz.
[docs/plan.md](docs/plan.md).

## Mimari

- **Backend:** `applications/backend` — Java 21, Spring Boot 3.x
  (web, security, data-jpa, mysql-connector-j).
- **Frontend:** `applications/frontend` — React 18 + Vite, backend'e
  `/api` altında proxy'lenen sade bir `fetch` istemcisi (JWT/token katmanı yok,
  cookie tabanlı session).
- **Altyapı:** `infra.yml` ile Docker Compose üzerinden MySQL.
- **Görevler:** `Taskfile.yml` ile `task` üzerinden çalıştırılır.

## Gereksinimler

- Java 21 (`sdk install java 21.0.2-tem` veya eşdeğeri)
- Maven Wrapper (repo içinde, ayrıca kurulum gerekmez)
- Node.js 18+ ve npm
- Docker + Docker Compose
- [go-task](https://taskfile.dev) (`brew install go-task`)

## Çalıştırma

```bash
# 1) MySQL'i ayağa kaldır
task infra:up

# 2) Backend'i başlat (http://localhost:8080)
task backend:run

# 3) Frontend'i başlat (http://localhost:5173)
task frontend:run
```

Frontend açıldığında "Check backend health" butonuna basarak backend'e
`/api/health` isteği atabilirsin; Vite dev server bu isteği proxy ile
`http://localhost:8080/api/health` adresine yönlendirir.

## Test

```bash
task backend:test
```

Backend testleri JPA context'i ayağa kaldırdığı için `task infra:up` ile
MySQL'in çalışıyor olması gerekir.

## Görevler

| Görev              | Açıklama                                   |
| ------------------ | ------------------------------------------- |
| `task infra:up`    | MySQL container'ını başlatır                |
| `task infra:down`  | MySQL container'ını durdurur                |
| `task backend:run` | Spring Boot backend'i çalıştırır            |
| `task backend:test`| Backend testlerini çalıştırır               |
| `task frontend:run`| Vite dev server'ı çalıştırır                |

## Session Kaybını Simüle Etme (UC-004, UC-005)

"Remember Me" ile giriş yaptıktan sonra oturum kaybını iki şekilde tetikleyip
otomatik girişi gözlemleyebilirsin:

1. **`JSESSIONID` cookie'sini silmek:** Tarayıcı DevTools → Application/Storage
   → Cookies → `JSESSIONID` satırını sil, sayfayı yenile. `notes-rm`
   cookie'si duruyorsa istek yine de kabul edilir (yeni bir session açılır) -
   yeniden login istenmez. (Faz 9'dan önce bu cookie'nin adı Spring'in
   varsayılanı olan `remember-me` idi - bkz. "Özel Cookie ve Parametre
   İsimleri" bölümü.)
2. **Backend'i yeniden başlatmak:** Session'lar bellekte tutulduğu için
   `task backend:run`'ı durdurup tekrar başlatmak tüm session'ları siler.
   Remember-me cookie'si bundan etkilenmez, çünkü stateless bir HMAC token'dır
   (imza `app.remember-me.key`'e bağlıdır, sunucu belleğinde tutulan bir kayıt
   değildir) - `app.remember-me.key` sabit kaldığı sürece restart sonrası da
   geçerlidir.

Süresi dolmuş cookie davranışını (UC-005/BR-007) elle gözlemlemek için
`applications/backend/src/main/resources/application.properties` içindeki
`app.remember-me.token-validity-seconds`'ı geçici olarak kısalt (ör. `30`),
backend'i yeniden başlat, "Remember Me" ile giriş yap, süre + birkaç saniye
bekle, `JSESSIONID`'yi sil ve korumalı bir uca istek at - istek login sayfasına
düşmeli. Otomatik test kapsamı için bkz.
`RememberMeAutoLoginAndExpiryTests`.

## Token Inspector, Rotasyon ve Çalıntı Cookie Tespiti (UC-011, UC-012, UC-013)

`app.remember-me.strategy=persistent` iken (bkz. UC-010) uygulama içinde bir
**Token Inspector** sayfası vardır (giriş ekranındaki veya Notlarım
sayfasındaki "Token Inspector" butonu, kimlik doğrulama gerektirmez -
`GET /api/token-inspector` bilinçli olarak `permitAll`, çünkü bu tek-kullanıcılı
demo'da başka kullanıcıdan gizlenecek bir şey yok ve UC-012'nin kendi
senaryosu tam da hırsızlık isteği reddedilirken Inspector'ın çalışmaya devam
etmesini bekliyor). Sayfa `persistent_logins` tablosundaki
kullanıcı adı/series/token/son kullanım kayıtlarını canlı listeler; "Yenile"
butonu veya sayfa açılışı her seferinde taze bir sorgu çalıştırır (BR-019 -
önbellek yok).

**Rotasyon ve çalıntı cookie tespiti bu fazda inşa edilmedi** - Spring
Security'nin `PersistentTokenBasedRememberMeServices`'i (Faz 6'da zaten
bağlı) bunu kendi başına yapıyor; bu faz sadece Inspector üzerinden
gözlemlenebilir hale getirdi ve otomatik testlerle doğruladı (bkz.
`TokenRotationTests`, `StolenCookieDetectionTests`,
`TokenInspectorPersistentModeTests`).

### Elle Rotasyon Gözlemleme (UC-011)

1. `app.remember-me.strategy=persistent` yap, backend'i yeniden başlat.
2. "Remember Me" ile giriş yap, Token Inspector'ı aç, series/token değerlerini not et.
3. `JSESSIONID` cookie'sini sil (DevTools → Application → Cookies), sayfayı
   **bir kez** yenile.
4. Token Inspector'ı tekrar aç: series aynı, token değişmiş olmalı (BR-015/016).

> **Dikkat (dev-mode uyarısı):** `main.jsx` React 18 `StrictMode` kullanır;
> bu, geliştirme modunda component effect'lerini bilinçli olarak iki kez
> tetikler (React'in kendi "eksik cleanup" tespiti). Sayfa yenilemesi
> `GET /api/auth-status`'u effect içinden çağırdığından, StrictMode bazen bu
> isteği neredeyse eş zamanlı olarak iki kez yollar - ikisi de tarayıcının
> o anki (henüz rotasyona uğramamış) tek kullanımlık remember-me cookie
> değerini taşır. İlk istek başarıyla auto-login yapıp token'ı döndürür;
> ikinci istek artık bayatlamış aynı token'ı sunar ve
> `PersistentTokenBasedRememberMeServices` bunu - haklı olarak, kendi
> kurallarına göre - hırsızlık sanıp **kaydı siler**. Bu, uygulamanın bir
> hatası değil, Barry Jaspan'in persistent-cookie algoritmasının doğal bir
> sonucu: aynı tek-kullanımlık cookie değerini taşıyan iki eşzamanlı istek,
> algoritma açısından "gerçek istemci + saldırgan" ile ayırt edilemez.
> Elle denerken bunu görürsen şaşırma - sayfayı tekrar yenilemek (StrictMode
> yarışı bu kez oluşmazsa) veya production build (`npm run build` + `npm run preview`,
> StrictMode'un devre dışı olduğu) ile tekrar denemek yeterli. Otomatik
> testler (`TokenRotationTests`) bu yarış durumuna hiç girmez, çünkü tek bir
> istek gönderirler.

### Elle Çalıntı Cookie Simülasyonu (UC-012)

Gerçek bir hırsızlığı simüle etmek için remember-me cookie değerinin bir
kopyasını çıkarıp, meşru tarayıcı onu rotasyona uğrattıktan **sonra**
tekrar oynatman (replay) yeterli:

1. **Cookie değerini kopyala** (DevTools → Application → Cookies →
   `notes-rm` satırının `Value` sütunu, ya da
   `document.cookie` HttpOnly olduğu için DevTools dışından okunamaz - sadece
   Application panelinden kopyalanabilir).
2. Meşru tarayıcıda `JSESSIONID`'yi sil ve **bir kez** bir korumalı uca istek
   at (auto-login) - bu, kopyaladığın değeri bayatlatır (token rotasyona
   uğrar, series aynı kalır).
3. Kopyaladığın (artık bayat) değeri `curl` ile geri oynat:

   ```bash
   curl -i -b "notes-rm=<kopyaladığın-değer>" http://localhost:8080/api/me
   ```

   Yanıt `401` olmalı ve `Set-Cookie: notes-rm=; Max-Age=0` ile cookie
   iptal edilmeli (BR-017).
4. Token Inspector'ı yenile: az önceki series'e ait kayıt tamamen silinmiş
   olmalı (BR-018).
5. Meşru tarayıcıda da `JSESSIONID`'yi sil ve sayfayı yenile: artık
   otomatik giriş yapamaz, login sayfasına düşer - series tümüyle iptal
   edildiği için meşru cookie de artık geçersiz.

**Önemli bulgu (BR-018'in tam kapsamı):** Spring Security'nin
`PersistentTokenBasedRememberMeServices.processAutoLoginCookie`'si, bir
token uyuşmazlığında `tokenRepository.removeUserTokens(username)` çağırır -
bu da (`JdbcTokenRepositoryImpl` üzerinden)
`delete from persistent_logins where username = ?` çalıştırır: silme
**kullanıcı** bazında, series bazında değil. BR-018'in Türkçe metni ("o
series'e bağlı tüm hatırlanma kayıtları") tek bir series'i işaret ediyor
gibi okunabilir, ama gerçek davranış daha geniş: kullanıcının **her
cihazdaki** tüm series'leri, hangisi çalınmış olursa olsun, tek seferde
iptal edilir. Bu lab'ın tek-cihazlı elle-yürütme senaryosunda (Test Adımı
1-4) iki davranış birbirinden ayırt edilemez (zaten tek bir series var),
ama gerçek kapsamı bilmek önemli - bkz. `StolenCookieDetectionTests`
(özellikle `findingSpringDeletesEveryDeviceSeriesForTheUserNotJustTheStolenOne`
testi, iki cihazı simüle edip bunu doğrudan kanıtlıyor).

## Özel Cookie ve Parametre İsimleri (UC-015)

Bu uygulama remember-me cookie'sini ve login formundaki "Beni hatırla"
parametresini Spring Security'nin ortak varsayılanı olan `remember-me`
yerine kendi özel isimleriyle çalıştırır:

- Cookie adı: `notes-rm` (`app.remember-me.cookie-name`)
- Parametre adı: `keep-me` (`app.remember-me.parameter-name`)

Her ikisi de `applications/backend/src/main/resources/application.properties`
içinde tanımlıdır ve `RememberMeNames` bean'i tarafından okunur - hem
`SecurityConfig`'in `rememberMe()` DSL'i hem de `AuthStatusController` aynı
bean'i kullanır, böylece iki farklı yerde birbirinden bağımsız yazılmış ama
"aynı" olması umulan iki string yerine tek bir kaynak vardır (BR-022).
Frontend tarafı da parametre adını hardcode etmez: `App.jsx`,
`GET /api/auth-status` yanıtındaki `rememberMeParameter` alanını okuyup
`LoginForm.jsx`'e prop olarak geçirir; form "Beni hatırla" kutusu
işaretliyken alanı bu isimle gönderir. Cookie adı frontend'e hiç
sızdırılmaz - `HttpOnly` olduğu ve `Set-Cookie` ile geldiği için tarayıcı
onu otomatik yönetir, frontend kodunun bilmesi gerekmez.

Her iki property de silinir/boş bırakılırsa (`app.remember-me.cookie-name`,
`app.remember-me.parameter-name`), `RememberMeNames` Spring'in kendi
varsayılanı olan `remember-me`'ye geri düşer - uygulama çökmez, sadece eski
davranışa döner (UC-015 A1). Formun yanlış/varsayılan bir parametre adı
göndermesi durumunda (UC-015 A2) - ör. backend `keep-me` beklerken form
`remember-me` gönderirse - giriş yine başarılı olur, sadece remember-me
cookie'si üretilmez; bu, Spring Security'nin remember-me filtresinin sadece
yapılandırılan tam parametre adını araması, başka bir şeyi hata saymaması
sayesinde kendiliğinden gerçekleşir - bkz. `RememberMeCustomNamesTests`.

DevTools'ta doğrulamak için: "Remember Me" ile giriş yap, Application →
Cookies altında `notes-rm` satırını gör; Network sekmesinde `/api/login`
isteğinin gövdesinde `keep-me=true` parametresini gör. Logout sonrası
`notes-rm` cookie'si `Max-Age=0` ile temizlenmiş olmalı (Faz 3'ün logout
davranışının bu özel isimle de çalıştığının regresyon kontrolü).

## Dokümantasyon

- [vision.md](docs/vision.md) — ürün vizyonu
- [requirements.md](docs/requirements.md) — fonksiyonel/teknik gereksinimler
- [plan.md](docs/plan.md) — faz faz uygulama planı
- [use_cases/](docs/use_cases/) — use case'ler
