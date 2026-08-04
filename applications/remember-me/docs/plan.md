# Remember-Me Lab — Uygulama Planı

Kaynak: [requirements.md](requirements.md) · Use case'ler: [use_cases/](use_cases/)

Kurallar:

- Her faz küçüktür ve tek bir hedefe odaklanır; bir faz bitmeden sonrakine geçilmez.
- Her fazın sonunda bir **Checkpoint** vardır: tamamlanan iş gözden geçirilir (kod review + doğrulama adımları çalıştırılır). Checkpoint geçilmeden faz "bitti" sayılmaz.
- "Doğrulanacak Sonuçlar" gözlemlenebilir çıktılardır; "Test Adımları" bu sonuçları üreten somut adımlardır.
- Stack sabittir (C-001…C-007): Java 21, Spring Boot 3.x + Spring Security 6, JPA/MySQL, Docker Compose (`infra.yml`), React 18 + Vite, Maven + npm + Taskfile, tek `USER` rolü.
- Dizin yapısı: backend `applications/remember-me/applications/backend`, frontend `applications/remember-me/applications/frontend` altında yer alır; `infra.yml` ve `Taskfile.yml` `applications/remember-me/` kökünde kalır.

---

## Faz 0 — Altyapı ve İskelet

**Hedef:** Boş ama çalışan bir geliştirme ortamı: MySQL ayakta, backend ve frontend başlıyor, tüm komutlar `task` üzerinden çalışıyor.

**Kapsam:** C-001, C-002, C-003, C-004, C-005, C-006

**İş Adımları:**

1. `infra.yml` ile MySQL'i Docker Compose üzerinden tanımla.
2. `applications/remember-me/applications/backend` altında Spring Boot 3.x iskeleti (Java 21, web + security + data-jpa + mysql bağımlılıkları), MySQL bağlantı ayarları.
3. `applications/remember-me/applications/frontend` altında React 18 + Vite iskeleti; backend'e proxy'li sade `fetch` altyapısı (JWT/token katmanı yok).
4. `Taskfile.yml`: `task infra:up`, `task backend:run`, `task frontend:run`, `task backend:test` görevleri.

**Checkpoint 0 (Review):**

- [ ] `infra.yml`, Taskfile ve proje yapısı gözden geçirildi; workspace'teki diğer uygulamalarla tutarlı.
- [ ] Bağımlılık listesinde gereksiz kütüphane yok.

**Doğrulanacak Sonuçlar:**

- MySQL container'ı sağlıklı çalışıyor; backend hatasız açılıyor ve DB'ye bağlanıyor.
- Frontend açılıyor ve backend'e istek atabiliyor.

**Test Adımları:**

1. `task infra:up` → `docker ps` ile MySQL'in ayakta olduğunu gör.
2. `task backend:run` → log'da hatasız açılış ve DB bağlantısını gör.
3. `task frontend:run` → tarayıcıda sayfanın açıldığını gör.
4. Frontend'den basit bir sağlık isteği at; yanıt geldiğini gör.

---

## Faz 1 — Form Login (UC-001)

**Hedef:** Demo kullanıcısı React formu üzerinden kullanıcı adı/şifre ile giriş yapabiliyor; şifreler güvenli saklanıyor.

**Kapsam:** FR-001, NFR-001 (kısmen: `JSESSIONID` HttpOnly), NFR-002, C-007

**İş Adımları:**

1. `User` entity + JPA repository; başlangıçta seed edilen tek demo kullanıcı (`USER` rolü).
2. BCrypt (strength ≥ 10) ile şifre saklama.
3. Spring Security form login yapılandırması (session tabanlı, React formundan gönderim).
4. React login sayfası: kullanıcı adı, şifre, hata mesajı gösterimi.

**Checkpoint 1 (Review):**

- [ ] Security yapılandırması gözden geçirildi: sadece login/statik uçlar açık, kalanı korumalı.
- [ ] UC-001 ana senaryo ve A1/A2 alternatif akışları kodla karşılaştırıldı.
- [ ] Şifrenin hiçbir yerde düz metin persist edilmediği doğrulandı (BR-002: genel hata mesajı dahil).

**Doğrulanacak Sonuçlar:**

- Doğru bilgilerle giriş başarılı, yanlış bilgilerle genel hata mesajı (hangi alanın yanlış olduğu söylenmiyor).
- DB'deki şifre alanı BCrypt formatında.
- `JSESSIONID` cookie'si `HttpOnly`.

**Test Adımları:**

1. Doğru kullanıcı adı/şifre ile giriş → korumalı sayfaya yönlenme.
2. Yanlış şifre ile giriş → genel hata mesajı, şifre alanı temiz.
3. DB'de `users` satırına bak → şifre `$2a$...` (BCrypt) formatında.
4. Tarayıcı DevTools → `JSESSIONID` cookie'sinde HttpOnly işaretli.
5. Login olmadan korumalı bir uca istek → reddediliyor.
6. Otomatik test: doğru/yanlış kimlik bilgisiyle login denemesi (MockMvc/integration).

---

## Faz 2 — Not Yönetimi (UC-006)

**Hedef:** Giriş yapan kullanıcı notlarını oluşturup listeleyebiliyor, güncelleyip silebiliyor — remember-me'yi deneyecek gerçek akış hazır.

**Kapsam:** FR-006

**İş Adımları:**

1. `Note` entity + repository + CRUD uçları (kullanıcıya bağlı).
2. React notlar sayfası: liste, oluştur, düzenle, sil.
3. Sahiplik denetimi: kullanıcı yalnızca kendi notlarını görür/yönetir (BR-008).

**Checkpoint 2 (Review):**

- [ ] Sahiplik denetimi her uçta var; UC-006 A1–A3 akışları kodla karşılaştırıldı.
- [ ] Boş başlık gibi geçersiz girdi backend'de reddediliyor (yalnızca frontend doğrulamasına güvenilmiyor).

**Doğrulanacak Sonuçlar:**

- CRUD dört işlem de çalışıyor ve kalıcı.
- Geçersiz not (boş başlık) kaydedilmiyor; hata mesajı görünüyor.

**Test Adımları:**

1. Not oluştur → listede görünüyor; sayfa yenilenince duruyor (kalıcılık).
2. Notu düzenle ve sil → liste güncelleniyor.
3. Boş başlıkla kaydet → hata, kayıt yok.
4. Otomatik test: CRUD + sahiplik (başka kullanıcının notuna erişim 403/404).

---

## Faz 3 — Remember-Me Opt-In (Token-Based) ve Logout (UC-002, UC-003)

**Hedef:** "Remember Me" checkbox'ı yalnızca işaretlendiğinde remember-me cookie'si üretiliyor; logout her iki cookie'yi de geçersiz kılıyor.

**Kapsam:** FR-002, FR-003, NFR-001 (tamamı)

**İş Adımları:**

1. Token-based remember-me yapılandırması (varsayılan strateji olarak).
2. Login formuna "Remember Me" checkbox'ı; parametrenin backend'e iletilmesi.
3. Logout yapılandırması: session invalidation + `JSESSIONID` ve remember-me cookie'lerinin silinmesi.

**Checkpoint 3 (Review):**

- [ ] Checkbox işaretli değilken cookie üretilmediği kod ve testle doğrulandı (BR-003).
- [ ] UC-002 ve UC-003 akışları (A1 dahil) kodla karşılaştırıldı.
- [ ] Her iki cookie HttpOnly (NFR-001 kapanışı).

**Doğrulanacak Sonuçlar:**

- Checkbox işaretli → remember-me cookie'si var; işaretsiz → yok.
- Logout sonrası iki cookie de geçersiz; korumalı istek login'e düşüyor.

**Test Adımları:**

1. Checkbox işaretsiz login → DevTools'ta sadece `JSESSIONID` var.
2. Checkbox işaretli login → remember-me cookie'si de var, HttpOnly.
3. Logout → cookie'ler silinmiş/geçersiz; korumalı sayfa login'e yönlendiriyor.
4. Otomatik test: rememberMe paramlı/paramsız login yanıtındaki `Set-Cookie` kontrolü; logout sonrası erişim reddi.

---

## Faz 4 — Otomatik Giriş ve Token Expiry (UC-004, UC-005)

**Hedef:** Session öldüğünde remember-me cookie'siyle otomatik yeniden doğrulama çalışıyor; süresi dolmuş cookie reddediliyor.

**Kapsam:** FR-004, FR-005

**İş Adımları:**

1. `tokenValiditySeconds` yapılandırılabilir hale getirilir (kısa değerle denenebilecek şekilde).
2. Session kaybı senaryosunun kolay tetiklenmesi (dokümante edilmiş yöntem: `JSESSIONID` silme / sunucu restart).

**Checkpoint 4 (Review):**

- [ ] UC-004 (A1, A2) ve UC-005 akışları davranışla karşılaştırıldı.
- [ ] Otomatik girişin "Remembered" seviyesi ürettiği (fully auth değil) doğrulandı — Faz 5'in ön şartı.

**Doğrulanacak Sonuçlar:**

- Session yokken geçerli cookie ile korumalı sayfa açılıyor (yeniden login yok).
- Geçerlilik süresi dolmuş cookie ile istek login sayfasına düşüyor.

**Test Adımları:**

1. Remember-me ile login → DevTools'tan `JSESSIONID` sil → korumalı sayfayı yenile → içerik açılıyor.
2. Aynısını remember-me cookie'sini de silerek yap → login'e yönlenme.
3. `tokenValiditySeconds`'ı 30 sn yap → login → 30+ sn bekle → session sil → istek → login'e yönlenme.
4. Otomatik test: geçerli/expired remember-me cookie'siyle istek simülasyonu.

---

## Faz 5 — Auth Seviyesi, Hassas Sayfa, Yeniden Doğrulama (UC-007, UC-008, UC-009)

**Hedef:** UI anlık auth seviyesini gösteriyor; Account Settings yalnızca full authentication ile açılıyor; remembered kullanıcı şifreyle seviye yükseltebiliyor.

**Kapsam:** FR-007, FR-008, FR-009

**İş Adımları:**

1. Auth durumunu dönen bir uç (Anonymous / Remembered / Fully Authenticated) + React'te sürekli görünen gösterge.
2. Account Settings sayfası; `isFullyAuthenticated()` kuralıyla koruma.
3. Yeniden doğrulama sayfası: şifre girilince seviyenin yükselmesi ve hedef sayfaya dönüş.

**Checkpoint 5 (Review):**

- [ ] UC-007/008/009 akışları (tüm alternatifler) kodla karşılaştırıldı.
- [ ] Remembered → Account Settings engeli backend'de (yalnızca UI gizlemesi değil).
- [ ] Yükseltmenin yalnızca aynı hesabın şifresiyle olduğu doğrulandı (BR-012).

**Doğrulanacak Sonuçlar:**

- Gösterge üç durumda da doğru: anonimken Anonymous, login sonrası Fully Authenticated, otomatik giriş sonrası Remembered.
- Remembered kullanıcı Account Settings'e giremiyor; şifre girince girebiliyor.

**Test Adımları:**

1. Anonim aç → gösterge "Anonymous". Login → "Fully Authenticated".
2. `JSESSIONID` sil, sayfayı yenile → "Remembered".
3. Remembered iken Account Settings → yeniden doğrulama sayfasına yönlenme.
4. Yanlış şifre → hata, seviye değişmiyor. Doğru şifre → "Fully Authenticated" + Account Settings açılıyor.
5. Otomatik test: remembered authentication ile Account Settings ucuna istek → 403/redirect; re-auth sonrası → 200.

---

## Faz 6 — Persistent Strateji ve Config ile Geçiş (UC-010)

**Hedef:** `persistent_logins` tablolu persistent-based remember-me çalışıyor; token-based ↔ persistent geçişi yalnızca konfigürasyonla (0 satır Java değişikliği) yapılıyor.

**Kapsam:** FR-010, NFR-005

**İş Adımları:**

1. `persistent_logins` tablosu (schema/migration) + `PersistentTokenRepository` yapılandırması.
2. Strateji seçimini property'ye bağla (ör. `app.remember-me.strategy=token|persistent`); iki modu da aynı kodla ayağa kaldıran koşullu konfigürasyon.

**Checkpoint 6 (Review):**

- [ ] Geçişin gerçekten sadece property değişikliği olduğu doğrulandı: mod değişiminde `git diff` boş (NFR-005).
- [ ] UC-010 akışları (A1, A2) davranışla karşılaştırıldı.

**Doğrulanacak Sonuçlar:**

- Persistent modda login sonrası `persistent_logins` tablosunda satır oluşuyor; token modda oluşmuyor.
- Eski moddan kalan cookie yeni modda sessizce reddedilip login isteniyor (hata/patlama yok).

**Test Adımları:**

1. `strategy=persistent` + restart → remember-me login → DB'de `persistent_logins` satırı var.
2. `strategy=token` + restart → remember-me login → tabloya yeni satır yok.
3. Mod değiştir, eski cookie ile istek at → login sayfası, 500 yok.
4. Otomatik test: her iki profil ile context ayağa kalkıyor; persistent profilde kayıt oluşuyor.

---

## Faz 7 — Token Inspector, Rotasyon ve Çalıntı Cookie Tespiti (UC-011, UC-012, UC-013)

**Hedef:** Token Inspector `persistent_logins` durumunu canlı gösteriyor; rotasyon (token döner, series sabit) ve çalıntı cookie tespiti (series iptali) gözlemlenebiliyor.

**Kapsam:** FR-011, FR-012, FR-013, NFR-004

**İş Adımları:**

1. Token Inspector sayfası + kayıtları (username, series, token, last_used) dönen uç.
2. Rotasyon ve hırsızlık senaryosunun Inspector üzerinden izlenebilmesi (gerekirse cookie kopyalama adımlarını dokümante et).

**Checkpoint 7 (Review):**

- [ ] UC-011/012/013 akışları uçtan uca elle çalıştırıldı ve gözlemler use case'lerdeki beklentilerle eşleşti.
- [ ] Inspector'ın tazeliği ölçüldü (yenileme sonrası ≤ 2 sn, NFR-004).

**Doğrulanacak Sonuçlar:**

- Her otomatik girişte `token` değeri değişiyor, `series` sabit (BR-015/016).
- Bayat token'lı istek: kullanıcının tüm series'lerine ait kayıtlar siliniyor (BR-018, Spring Security'nin yerleşik `removeUserTokens` davranışı — yalnızca çalınan series değil), istek reddediliyor, meşru tarayıcı da yeniden login'e düşüyor (BR-017/018).
- Inspector, tablo durumunu yenileme sonrası ≤ 2 sn içinde yansıtıyor.

**Test Adımları:**

1. Persistent modda remember-me login → Inspector'da series/token'ı not et.
2. `JSESSIONID` sil → sayfa yenile (otomatik giriş) → Inspector'da token değişti, series aynı.
3. Remember-me cookie değerini kopyala → bir otomatik giriş daha tetikle (kopya bayatlar) → kopya değeri cookie'ye geri yaz → istek at → reddedildi, Inspector'da kullanıcının tüm series kayıtları silindi.
4. Meşru tarayıcıda sayfa yenile → login sayfası (series iptalinin etkisi).
5. Otomatik test: `PersistentTokenRepository` üzerinden rotasyon ve `CookieTheftException` senaryosu.

---

## Faz 8 — Süresi Dolmuş Kayıt Temizliği (UC-014)

**Hedef:** Zamanlanmış iş süresi dolmuş `persistent_logins` satırlarını siliyor; DB hatası uygulamayı düşürmüyor.

**Kapsam:** FR-014, NFR-003

**İş Adımları:**

1. Zamanlanmış temizlik işi (`@Scheduled`): `last_used + tokenValiditySeconds` geçmiş satırları sil.
2. Hata dayanıklılığı: DB erişilemezse logla ve devam et.

**Checkpoint 8 (Review):**

- [ ] Silme kriteri gözden geçirildi: yalnızca süresi dolmuş satırlar (BR-020).
- [ ] UC-014 A1 (DB kapalı) senaryosu elle test edildi; uygulama istek karşılamaya devam etti (BR-021).

**Doğrulanacak Sonuçlar:**

- Süresi dolmuş satırlar planlı çalışmada siliniyor; geçerli satırlar duruyor.
- MySQL kapalıyken iş hata logluyor; HTTP istekleri (DB gerektirmeyenler) çalışmaya devam ediyor, restart gerekmiyor.

**Test Adımları:**

1. Kısa validity ile birkaç kayıt oluştur → süre dolsun → iş çalışsın (test için kısa cron) → Inspector'da satırlar silindi.
2. Geçerli bir kayıt varken işi çalıştır → satır duruyor.
3. `docker stop mysql` → işin çalışmasını bekle → log'da hata var, uygulama ayakta → `docker start mysql` → sonraki çalışmada temizlik yapılıyor.
4. Otomatik test: temizlik sorgusunun sadece expired satırları sildiği; hata durumunda exception'ın yutulup loglandığı.

---

## Faz 9 — Özel Cookie ve Parametre İsimleri (UC-015)

**Hedef:** Remember-me cookie'si `notes-rm`, form parametresi `keep-me` adıyla çalışıyor; isimler konfigürasyondan yönetiliyor.

**Kapsam:** FR-015

**İş Adımları:**

1. Cookie ve parametre adlarını property'den okuyan yapılandırma; frontend'in aynı parametre adını göndermesi.
2. Logout'taki cookie temizliğinin özel adı da kapsaması.

**Checkpoint 9 (Review):**

- [ ] Frontend/backend isim eşleşmesi tek kaynaktan (config) geliyor; UC-015 A1/A2 kodla karşılaştırıldı.
- [ ] Logout özel adlı cookie'yi de siliyor (Faz 3 regresyon kontrolü).

**Doğrulanacak Sonuçlar:**

- DevTools'ta cookie adı `notes-rm`; login isteği `keep-me` parametresi taşıyor.
- Otomatik giriş ve logout özel isimlerle sorunsuz.

**Test Adımları:**

1. Remember-me login → DevTools'ta `notes-rm` cookie'si; network sekmesinde `keep-me` parametresi.
2. `JSESSIONID` sil → otomatik giriş çalışıyor.
3. Logout → `notes-rm` silinmiş.
4. Otomatik test: `Set-Cookie` başlığında özel ad; özel paramla remember-me tetikleniyor, varsayılan adla tetiklenmiyor.

---

## Faz 10 — IP-Bound Remember-Me ve IP Görünürlüğü (UC-016, UC-017)

**Hedef:** Remember-me token'ı üretildiği istemci IP'sine bağlı; farklı IP'den replay reddediliyor ve bağlı IP, Token Inspector'da görünüyor.

**Kapsam:** FR-016, FR-017

**İş Adımları:**

1. Kayıt oluştururken istemci IP'sini sakla; otomatik girişte istek IP'siyle karşılaştır, uyuşmazsa reddet. Özellik property ile aç/kapa.
2. Token Inspector'a "bağlı IP" sütunu; binding kapalıysa bunu açıkça göster.
3. Farklı IP simülasyon yönteminin dokümante edilmesi (ör. `localhost` vs `127.0.0.1` / ikinci makine / header'sız doğrudan erişim).

**Checkpoint 10 (Review):**

- [ ] UC-016 (A1, A2) ve UC-017 akışları elle uçtan uca çalıştırıldı.
- [ ] IP kontrolünün yalnızca binding etkinken devrede olduğu doğrulandı (BR-023).
- [ ] Regresyon: binding kapalıyken Faz 3–9 davranışları değişmedi.

**Doğrulanacak Sonuçlar:**

- Aynı IP'den otomatik giriş çalışıyor; farklı IP'den istek reddedilip login isteniyor.
- Inspector her kayıtta bağlı IP'yi gösteriyor; kapalıyken "IP'ye bağlı değil" ibaresi var.

**Test Adımları:**

1. Binding açık → remember-me login → Inspector'da kaydın IP'si görünüyor.
2. Aynı istemciden `JSESSIONID` sil → otomatik giriş başarılı.
3. Cookie'yi farklı IP'li istemciye taşı → istek reddedildi, login sayfası.
4. Binding kapat + restart → farklı IP'den istek kabul ediliyor (kontrol devre dışı).
5. Otomatik test: IP eşleşen/eşleşmeyen isteklerde kabul/red davranışı.

---

## Kapanış — Son Gözden Geçirme

**Hedef:** Tüm gereksinimlerin karşılandığının toplu doğrulaması.

**Checkpoint (Final Review):**

- [ ] requirements.md'deki her FR/NFR/C satırı için ilgili fazın checkpoint'i işaretli; Status alanları güncellendi.
- [ ] Tüm otomatik testler yeşil (`task backend:test`).
- [ ] UC-001…UC-017 ana senaryoları temiz bir ortamda (`task infra:up`'tan itibaren) baştan sona elle koşuldu.
- [ ] README/Taskfile ile projeyi sıfırdan ayağa kaldırma adımları doğrulandı.
