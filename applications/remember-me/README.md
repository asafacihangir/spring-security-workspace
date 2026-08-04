# Remember-Me Lab

Spring Security'nin token-based ve persistent-token remember-me stratejilerini
uçtan uca deneyebileceğimiz bir demo uygulama. Kullanıcı adı/şifre ile giriş
yapar, notlarını yönetir, "Remember Me" ile oturumu hayatta tutar ve
Token Inspector üzerinden arka planda ne olduğunu izler.

Tüm 11 faz tamamlandı (bkz. [docs/plan.md](docs/plan.md)). Bu uygulama
aşağıdakilerin hepsini uçtan uca gösterir:

- Form login (kullanıcı adı/şifre) ve BCrypt ile güvenli şifre saklama.
- İsteğe bağlı "Remember Me" ile **token-based** (stateless, HMAC imzalı)
  veya **persistent** (veritabanı destekli, `persistent_logins` tablosu)
  remember-me - ikisi arasında geçiş tamamen konfigürasyonla yapılır (bkz.
  "Strateji Seçimi" bölümü).
- Logout: hem oturumu hem remember-me cookie'sini/kaydını geçersiz kılar.
- UI'da anlık authentication seviyesi göstergesi (Anonymous / Remembered /
  Fully Authenticated).
- `isFullyAuthenticated()` ile korunan Account Settings sayfası ve
  remembered bir oturumu şifreyle yeniden yükselten re-authentication akışı.
- Persistent moddaki kayıtları canlı gösteren bir **Token Inspector** sayfası
  (kimlik doğrulama gerektirmez - bkz. aşağıdaki uyarı).
- Token rotasyonu (her otomatik girişte token değişir, series sabit kalır)
  ve çalıntı cookie tespiti (bayat token → tüm series'ler iptal).
- Süresi dolmuş `persistent_logins` satırlarını temizleyen zamanlanmış bir
  arka plan işi.
- Özel remember-me cookie/parametre isimleri (`notes-rm` / `keep-me`).
- İsteğe bağlı IP-bound remember-me ve Token Inspector'da IP görünürlüğü.
- CSRF koruması (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header), tüm
  mutating (POST/PUT/DELETE) uçlarda etkin.

Faz faz nasıl inşa edildiği için bkz. [docs/plan.md](docs/plan.md); her
use case'in tam senaryosu için bkz. [docs/use_cases/](docs/use_cases/).

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

Frontend açıldığında (`http://localhost:5173`) bir login formu (kullanıcı
adı, şifre, "Beni hatırla" kutusu) ve altında bir "Token Inspector" butonu
görürsün. Giriş için demo hesabı kullan:

- **Kullanıcı adı:** `demo`
- **Şifre:** `password123`

Bu hesap `DemoUserSeeder` tarafından backend her başladığında otomatik
olarak seed edilir (henüz yoksa oluşturulur - bkz.
`DemoUserSeeder.java`); ayrıca elle bir kayıt oluşturman gerekmez.

## Strateji Seçimi (UC-010)

Bu uygulama iki remember-me stratejisini tek bir property ile değiştirir -
`applications/backend/src/main/resources/application.properties` içindeki
`app.remember-me.strategy`:

- **`token`** (varsayılan) - stateless, HMAC imzalı cookie
  (`TokenBasedRememberMeServices`). Sunucu tarafında hiçbir kayıt tutulmaz;
  Token Inspector, Rotasyon (UC-011), Çalıntı Cookie Tespiti (UC-012) ve
  IP-Bound Remember-Me (UC-016/UC-017) bölümlerinin hepsi bu modda anlamsız
  kalır (Inspector "token-based strateji aktif" mesajı gösterir).
- **`persistent`** - veritabanı destekli cookie
  (`PersistentTokenBasedRememberMeServices`), `persistent_logins` tablosuna
  yazar. Aşağıdaki Token Inspector, Rotasyon, Çalıntı Cookie Tespiti ve
  IP-Bound bölümlerinin **hepsi bu modu varsayar** - onları denemeden önce
  bu property'yi `persistent` yap ve backend'i yeniden başlat.

Geçiş tek satırlık bir konfigürasyon değişikliğidir, kod değişikliği
gerektirmez (NFR-005) - bkz. [UC-010](docs/use_cases/UC-010-strategy-switching.md).

## Test

```bash
task backend:test
```

Backend testleri JPA context'i ayağa kaldırdığı için `task infra:up` ile
MySQL'in çalışıyor olması gerekir.

## Görevler

| Görev                  | Açıklama                                          |
| ---------------------- | -------------------------------------------------- |
| `task infra:up`        | MySQL container'ını başlatır                       |
| `task infra:down`      | MySQL container'ını durdurur                       |
| `task backend:run`     | Spring Boot backend'i çalıştırır                   |
| `task backend:test`    | Backend testlerini çalıştırır                      |
| `task frontend:run`    | Vite dev server'ı çalıştırır                       |
| `task frontend:build`  | Frontend'i production için derler (`vite build`)   |

## CSRF Koruması

Bu uygulama CSRF korumasını etkin çalıştırır -
`CookieCsrfTokenRepository.withHttpOnlyFalse()` ile üretilen `XSRF-TOKEN`
cookie'si JS tarafından okunabilir (bilerek `HttpOnly` değil - bu bir kimlik
bilgisi değildir, `JSESSIONID`/remember-me cookie'sinin aksine), frontend
(`api.js`) bu değeri okuyup her mutating (POST/PUT/DELETE) istekte
`X-XSRF-TOKEN` header'ı olarak geri gönderir. GET istekleri bu header'a
ihtiyaç duymaz. Backend tarafında `CsrfCookieFilter` (bkz. javadoc'u), saf
bir JSON API'de bu cookie'nin hiç yazılmayacağı - Spring Security'nin
CSRF token'ı yalnızca bir view render edildiğinde tetiklenen "deferred"
(tembel) bir mekanizmayla ürettiği - gerçek gotcha'sını kapatır.

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

**Not (UC-003, logout'un gerçek kapsamı):** logout her zaman tarayıcıdaki
cookie'leri doğru şekilde temizler, ama sunucu tarafında gerçekten iptal
edilen bir kayıt yalnızca `persistent` modda vardır - `token` modda (bu
uygulamanın varsayılanı) silinecek bir sunucu kaydı yoktur, bu yüzden
logout'tan önce kopyalanmış ham bir cookie değeri, imzası süresi dolana
kadar hâlâ kriptografik olarak geçerlidir (bkz.
`RememberMeAndLogoutTests.knownLimitationAReplayedPreLogoutRememberMeCookieValueStillAuthenticates`).
`persistent` moda geçmek (UC-010) bu boşluğu kalıcı olarak kapatır - bkz.
`PersistentModeLogoutRevokesTokensTests` ve
[UC-003](docs/use_cases/UC-003-logout.md)'ün "Step 4'ün Kapsamı" notu.

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
`GET /api/token-inspector` bilinçli olarak `permitAll`). Sayfa
`persistent_logins` tablosundaki kullanıcı adı/series/token/son kullanım
kayıtlarını canlı listeler; "Yenile" butonu veya sayfa açılışı her seferinde
taze bir sorgu çalıştırır (BR-019 - önbellek yok).

> **Uyarı:** bu sayfa gerçek, canlı remember-me kimlik bilgilerini (series +
> token, ki birlikte tam remember-me cookie değerini yeniden üretirler)
> hiçbir kimlik doğrulama olmadan gösterir. Bu yalnızca UC-012'nin çalıntı
> cookie senaryosu, bir öğrenenin gerçek bir cookie değerini kopyalamasını
> gerektirdiği için böyle bırakıldı - **bu desen production'da asla
> kullanılmamalıdır.** Gerçek bir "admin" görünümü güçlü kimlik doğrulama,
> yetki sınırlaması gerektirir ve ham token değerlerini asla döndürmez -
> bkz. `TokenInspectorController`'ın javadoc'u.

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

## IP-Bound Remember-Me (UC-016, UC-017)

`app.remember-me.strategy=persistent` iken (bkz. yukarısı), ayrıca
`app.remember-me.ip-binding-enabled=true` yapılırsa (varsayılan `false`),
her yeni "Remember Me" girişi **istemci IP'sine bağlanır**
(`persistent_logins.bound_ip`): sonraki her otomatik girişte istek IP'si bu
kayıtla karşılaştırılır, uyuşmuyorsa istek reddedilir (temiz bir 401 - login
sayfasına düşer, 500 değil) ve login yeniden istenir (BR-023). Bu, Faz 6/7'nin
persistent stratejisi üzerine inşa edilen ve Spring Security'de hazır bir
karşılığı olmayan tamamen özel bir mantıktır - bkz.
`IpBoundPersistentTokenBasedRememberMeServices`'in javadoc'u.

**Kapsam:** yalnızca `persistent` strateji için - token-based (stateless,
HMAC-imzalı) cookie'nin sunucu tarafında hiçbir kaydı yok, bağlanacak bir IP
tutacak yer de yok; `token` stratejisi bu özellikten tamamen etkilenmedi
(`app.remember-me.ip-binding-enabled`'ın o modda hiçbir etkisi yok - bkz.
`IpBindingDisabledTests`).

**IP kaynağı:** `HttpServletRequest.getRemoteAddr()` - `X-Forwarded-For` gibi
proxy başlıkları değil, çünkü bu uygulamanın önünde güvenilir bir reverse
proxy yok; o başlığı güvenmek istemcinin kendi IP'sini iddia etmesine izin
verir ve bağlamanın amacını tamamen ortadan kaldırırdı.

Token Inspector'da yeni bir **"Bağlı IP"** sütunu var: binding açıkken ve
kayıt bir IP'ye bağlıyken o IP görünür; binding kapalıyken (veya kayıt bu
özellikten önce oluşturulmuşsa) hücre asla boş bırakılmaz - açıkça
**"IP'ye bağlı değil"** yazar (BR-024, UC-017 A1).

### Farklı IP Simülasyonu - Elle Test İçin Ne Gerçekten Çalışıyor

Bu, bu lab'daki en zor elle-doğrulanacak senaryo: tek bir geliştirme
makinesinde **gerçekten** iki farklı istemci IP'si üretmek genellikle mümkün
değil. Aşağıdakiler bu ortamda fiilen denendi, sonuçlarıyla birlikte:

- **`localhost` vs `127.0.0.1` - bu makinede işe yaradı, ama garanti değil.**
  `curl http://localhost:8080/...` bu makinede `localhost`'u IPv6 loopback'e
  (`::1`) çözdü, `curl http://127.0.0.1:8080/...` ise IPv4 loopback'e
  (`127.0.0.1`) - Tomcat/Spring bu ikisini `getRemoteAddr()` üzerinden
  **gerçekten farklı iki string** olarak görüyor. Bu README'nin
  hazırlanması sırasında uçtan uca elle doğrulandı (`curl` ile): `keep-me=true`
  ile `localhost` üzerinden giriş → `persistent_logins.bound_ip` = `0:0:0:0:0:0:0:1`
  olarak kaydedildi; aynı cookie ile tekrar `localhost` üzerinden otomatik
  giriş → kabul edildi (200, token rotasyona uğradı); rotasyona uğramış
  cookie ile `127.0.0.1` üzerinden istek → reddedildi (401, `notes-rm`
  cookie'si iptal edildi, DB'deki token/seri **değişmeden** kaldı); hemen
  ardından aynı cookiye `localhost`'tan tekrar istek → yine kabul edildi
  (reddedilen çapraz-IP denemesinin meşru sahibin kaydını bozmadığının
  kanıtı - bkz. `IpBoundPersistentTokenBasedRememberMeServices.processAutoLoginCookie`'nin
  javadoc'undaki "Ordering" bölümü). **Ama bu, işletim sistemi/DNS
  çözümlemesinin `localhost`'u IPv6'ya, `127.0.0.1`'i IPv4'e ayırmasına
  bağlı** - başka bir makinede (ör. IPv6 loopback'i devre dışı bir sistemde)
  ikisi de aynı stringe (`127.0.0.1`) çözülebilir ve bu numara işe yaramaz;
  denemeden önce iki adresin gerçekten farklı `getRemoteAddr()` değeri
  ürettiğini (ör. Token Inspector'daki `boundIp` sütunundan) doğrula.
- **Tarayıcıdan elle:** tarayıcı sekmesinde `http://localhost:5173`
  (frontend, `/api` proxy'siyle backend'e `localhost:8080` üzerinden gider)
  ile giriş yap, sonra DevTools'tan `notes-rm` cookie değerini kopyalayıp
  `http://127.0.0.1:8080/api/me`'ye doğrudan `curl` ile (yukarıdaki gibi)
  oynat - saf tarayıcı-içi bir simülasyon pratik değil, çünkü tarayıcı aynı
  cookie'yi iki farklı origin'e (`localhost` vs `127.0.0.1`) otomatik
  taşımaz (ayrı origin'ler, ayrı cookie jar'ları); `curl` bu sınırı olmadan
  çalışır.
- **Denenmedi ama gerçek bir alternatif:** backend'i `server.address=0.0.0.0`
  ile başlatıp aynı ağdaki ikinci bir cihazdan (telefon, başka bir bilgisayar)
  makinenin LAN IP'sine istek atmak - bu, "gerçekten iki farklı istemli IP'si"
  üreten tek yöntemdir, ama bu ortamda ikinci bir cihaz mevcut olmadığı için
  bu rapor kapsamında fiilen denenmedi; yalnızca yukarıdaki `localhost` vs
  `127.0.0.1` yöntemi ve aşağıdaki otomatik testler fiilen çalıştırıldı.
- **Otomatik test - tek gerçekten güvenilir yöntem.** `IpBoundRememberMeTests`
  ve `IpBindingDisabledTests`, Spring'in
  `MockHttpServletRequestBuilder.remoteAddress(String)`'i ile
  `getRemoteAddr()`'ı doğrudan, gerçek bir ağ yolu gerekmeden sahtesini
  üreterek ayarlıyor - aynı/farklı IP karşılaştırmasını hiçbir ortam
  varsayımına bağlı olmadan, deterministik biçimde kapsıyorlar. Bu dosyaların
  kapsadığı senaryolar: kayıt oluşurken IP'nin yazılması, aynı IP'den
  otomatik giriş (A1), farklı IP'den red (ana senaryo/BR-023), reddedilen
  denemenin satırı/token'ı bozmadığı, Token Inspector'ın `boundIp`'i
  doğru yansıttığı, binding kapalıyken farklı IP'nin kabul edildiği (A2) ve
  `token` stratejisinde bu property'nin hiçbir etkisinin olmadığı
  (regresyon).

## Dokümantasyon

- [vision.md](docs/vision.md) — ürün vizyonu
- [requirements.md](docs/requirements.md) — fonksiyonel/teknik gereksinimler
- [plan.md](docs/plan.md) — faz faz uygulama planı
- [use_cases/](docs/use_cases/) — use case'ler
