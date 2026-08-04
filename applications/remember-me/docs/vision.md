# Remember-Me Lab — Vision

## Overview

Remember-Me Lab, kişisel notlarını tutan bir kullanıcıya hizmet eder. Kullanıcı notlarına her gün, farklı zamanlarda geri döner ve her seferinde kullanıcı adı ile şifresini yeniden girmek istemez; tarayıcısını kapatıp açtığında uygulamanın onu hatırlamasını, notlarının onu bekliyor olmasını bekler. Öte yandan hesap ayarları gibi hassas işlemler için bu rahatlığın bir bedeli olduğunu — hatırlanmış olmakla kimliğini gerçekten kanıtlamış olmanın aynı şey olmadığını — fark etmeden yaşar.

Uygulama, kullanıcıya notlarını tuttuğu tek bir yer sunar: login olur, isteğe bağlı "Remember Me" seçeneğiyle giriş yapar, notlarını yönetir ve session'ı kaybolduğunda kimlik bilgilerini yeniden girmeden kaldığı yerden devam eder. Hassas Account Settings sayfası ise yalnızca şifresiyle gerçekten doğrulanmış kullanıcılara açıktır.

Sistem aslında Spring Security'nin remember-me mekanizmasını uygulamalı öğrenmek için tasarlanmış bir laboratuvardır. Normalde perde arkasında kalan cookie ve token davranışı bilinçli olarak UI üzerinden görünür kılınmıştır; iki remember-me stratejisi de (token-based ve persistent-based) kapsanır ve öğrenen aralarında geçiş yapabilir. Yeni özellik icat etmekten çok her davranışın gözlemlenebilir olması önemsenir: çalıntı cookie simülasyonu, süresi dolmuş token temizliği ve hassas sayfa koruması gibi senaryoların hepsi bu amaca hizmet eder.

## Goals and Desired Outcomes

- Spring Security'nin remember-me mekanizmasını (hem token-based hem persistent-based) çalışan bir uygulama üzerinde uygulamalı öğretmek.
- Normalde perde arkasında kalan cookie/token davranışını (cookie üretimi, token rotasyonu, `persistent_logins` satırları) UI üzerinden gözlemlenebilir kılmak.
- Remember-me'nin güvenlik ödünleşimlerini deneyimletmek: remembered vs fully-authenticated ayrımı, çalıntı cookie tespiti, IP bağlama.
- Tüm remember-me senaryolarının küçük, çalışan bir uygulama üzerinde birebir denenebildiği bir lab ortamı sunmak.

**Desired outcome:** Uygulamayı baştan sona deneyen bir öğrenen; iki remember-me stratejisi arasındaki farkı, series/token rotasyonunun hırsızlığı nasıl tespit ettiğini ve `isFullyAuthenticated()` korumasının neden gerektiğini kendi gözlemlerine dayanarak açıklayabilir hale gelir.

## Business Capabilities

| Capability | Açıklama |
| --- | --- |
| Kimlik doğrulama & session yönetimi | Form login, logout, cookie tabanlı session (`JSESSIONID`) |
| Remember-me (çift strateji) | Token-based ve persistent-based modlar; aralarında geçiş yapılabilir |
| Kişisel not yönetimi | Login sonrası basit not CRUD'u — remember-me'yi denemek için taşıyıcı işlev |
| Authentication seviyesi görünürlüğü | UI'da anlık Remembered / Fully Authenticated durumu |
| Hassas sayfa koruması | Account Settings sayfası yalnızca fully-authenticated erişime açık |
| Token Inspector | `persistent_logins` satırlarının (series, token, last_used) canlı görüntülenmesi |
| Çalıntı cookie tespiti | Bayat token'la gelen isteklerde series iptali ve zorunlu re-login |
| Token temizliği | Süresi dolmuş `persistent_logins` satırlarının zamanlanmış silinmesi |
| Özelleştirme | Özel cookie/parametre isimleri, IP-bound remember-me |

## Users and Roles

| Kullanıcı | Rol | Amaç |
| --- | --- | --- |
| Öğrenen (geliştirici) | Uygulamayı çalıştıran gerçek kişi | Senaryoları uygulayıp DevTools/MySQL üzerinden davranışı gözlemler |
| Demo kullanıcısı | `USER` | Login, remember-me ve not yönetimi akışlarının test hesabı |
| Simüle saldırgan | Rol değil, senaryo | Eski cookie değerini tekrar oynatarak hırsızlık tespitini tetikler (Story 4) |

Uygulamada gerçek bir rol hiyerarşisi yoktur; tek `USER` rolü yeterlidir çünkü lab'ın odağı yetkilendirme değil, authentication seviyeleridir (anonymous → remembered → fully authenticated).

## Tech Stack

| Katman | Teknoloji |
| --- | --- |
| Dil | Java 21 |
| Backend | Spring Boot 3.x, Spring Security 6 |
| Persistence | Spring Data JPA (Hibernate), MySQL |
| Infra | MySQL, Docker ile `infra.yml` (docker compose) üzerinden ayağa kaldırılır |
| Frontend | React 18 + Vite, sade fetch API (session/cookie tabanlı, JWT yok) |
| Build | Maven (backend), npm (frontend) |
| Görev yönetimi | Taskfile — tüm geliştirme komutları (build, run, db) `task` üzerinden çalıştırılır |
| Dev araçları | MySQL client (tablo incelemesi), cookie incelemesi için tarayıcı DevTools |

React frontend, Spring Boot backend ile cookie tabanlı session üzerinden konuşur (`JSESSIONID` + `remember-me` cookie'leri) — remember-me tam olarak bu kurulum için tasarlanmıştır. Token/JWT katmanı bilinçli olarak eklenmemiştir; böylece tarayıcının cookie davranışı doğrudan gözlemlenebilir.

## User Scenarios & Testing

**User Story 1 – Login with Remember-Me (Priority: P1)**

Kullanıcı, React login formu üzerinden kullanıcı adı, şifre ve isteğe bağlı "Remember Me" checkbox'ı ile giriş yapar. Kutu işaretliyse backend, `JSESSIONID`'nin yanına bir `remember-me` cookie'si yazar.

**Why this priority:** Login akışı ve doğru `remember-me` HTTP parametresine bağlanmış checkbox olmadan diğer hiçbir senaryo test edilemez.

**Independent Test:** Checkbox işaretli login olup DevTools → Application → Cookies ekranında hem `JSESSIONID` hem `remember-me` cookie'lerinin varlığı doğrulanarak test edilebilir; checkbox işaretsiz login yalnızca `JSESSIONID` üretmelidir.

**Acceptance Scenarios:**

1. **Given** login sayfasındaki kayıtlı bir kullanıcı, **When** geçerli bilgilerle ve "Remember Me" işaretli olarak formu gönderdiğinde, **Then** yanıt hem `JSESSIONID` hem `remember-me` cookie'lerini set eder.
2. **Given** login sayfasındaki kayıtlı bir kullanıcı, **When** geçerli bilgilerle ama "Remember Me" işaretsiz olarak formu gönderdiğinde, **Then** yalnızca `JSESSIONID` set edilir.
3. **Given** geçersiz bilgilere sahip bir kullanıcı, **When** formu gönderdiğinde, **Then** login başarısız olur ve `remember-me` cookie'si üretilmez.
4. **Given** login olmuş bir kullanıcı, **When** Logout'a tıkladığında, **Then** her iki cookie de geçersiz kılınır ve bir sonraki istek login gerektirir.

---

**User Story 2 – Auto-Login After Session Loss (Priority: P1)**

"Remember Me" ile giriş yapmış bir kullanıcı, session'ı öldüğünde (tarayıcı yeniden başlatma veya `JSESSIONID` silme) bilgilerini yeniden girmeden otomatik olarak yeniden authenticate edilir.

**Why this priority:** Remember-me'nin asıl değeri budur; diğer her şey bunun üzerine kurulur.

**Independent Test:** Remember-me ile login olup DevTools'tan yalnızca `JSESSIONID` cookie'sini silerek, sayfa yenilendiğinde kullanıcının hâlâ authenticated olduğu ve yeni bir session aldığı doğrulanarak test edilebilir.

**Acceptance Scenarios:**

1. **Given** remember-me ile login olmuş bir kullanıcı, **When** `JSESSIONID` cookie'si silinip sayfayı yenilediğinde, **Then** otomatik olarak login edilir ve yeni bir `JSESSIONID` üretilir.
2. **Given** remember-me OLMADAN login olmuş bir kullanıcı, **When** `JSESSIONID` cookie'si silinip sayfayı yenilediğinde, **Then** login sayfasına yönlendirilir.
3. **Given** remember-me ile login olmuş bir kullanıcı, **When** `remember-me` cookie'si silinir ama `JSESSIONID` durursa, **Then** mevcut session süresi dolana kadar çalışmaya devam eder.
4. **Given** süresi dolmuş bir remember-me token'ı (`tokenValiditySeconds` aşılmış), **When** kullanıcı session'sız geri döndüğünde, **Then** otomatik login reddedilir ve login sayfası gösterilir.

---

**User Story 3 – Fully-Authenticated Protection for Sensitive Page (Priority: P2)**

Uygulamada, remembered kullanıcıların erişemediği bir "Account Settings" sayfası vardır; gerçek kullanıcı adı/şifre girişi gerektirir (`isFullyAuthenticated()`). UI, anlık authentication seviyesini (Remembered / Fully Authenticated) gösterir, böylece fark gözle görülür.

**Why this priority:** Remember-me'nin temel güvenlik ödünleşimini gösterir; Story 1–2'nin çalışıyor olmasına bağlıdır.

**Independent Test:** Form login sonrası sayfaya normal erişilerek, ardından `JSESSIONID` silinip ("remembered" duruma düşüp) normal sayfalar çalışırken bu sayfanın engellendiği doğrulanarak test edilebilir.

**Acceptance Scenarios:**

1. **Given** form üzerinden login olmuş bir kullanıcı, **When** Account Settings sayfasını açtığında, **Then** sayfa yüklenir.
2. **Given** remembered bir kullanıcı (cookie ile otomatik login olmuş), **When** Account Settings sayfasını açtığında, **Then** erişim reddedilir ve bilgilerini yeniden girmesi istenir.
3. **Given** remembered bir kullanıcı, **When** normal Notes sayfasını açtığında, **Then** sayfa normal şekilde yüklenir.
4. **Given** şifresiyle yeniden authenticate olmuş remembered bir kullanıcı, **When** Account Settings'i tekrar denediğinde, **Then** sayfa yüklenir.

---

**User Story 4 – Stolen Cookie Detection (Priority: P2)**

Persistent-based remember-me aktifken uygulama, tekrar oynatılan (çalınmış) bir cookie'yi tespit eder: geçerli bir series ama bayat bir token taşıyan istek, tüm series'i geçersiz kılar ve yeniden login zorunlu olur. "Token Inspector" sayfası, mevcut `persistent_logins` satırlarını (series, token, last_used) gösterir; böylece öğrenen eski bir cookie değerini kopyalayıp hırsızlığı simüle edebilir.

**Why this priority:** Persistent token'ların temel güvenlik avantajı ve uygulamanın en öğretici deneyi budur; Story 2'yi gerektirir.

**Independent Test:** Mevcut `remember-me` cookie değeri kopyalanıp, uygulamanın token'ı döndürmesi sağlanarak (`JSESSIONID` sil, yenile), eski cookie değeri DevTools'tan geri yapıştırılarak erişimin reddedildiği ve DB satırının silindiği doğrulanarak test edilebilir.

**Acceptance Scenarios:**

1. **Given** persistent remember-me ile otomatik login olan bir kullanıcı, **When** token kullanıldığında, **Then** `persistent_logins` tablosundaki `token` değeri değişir, `series` sabit kalır.
2. **Given** eski bir cookie'yi tekrar oynatan bir saldırgan (geçerli series, bayat token), **When** istek geldiğinde, **Then** o series'e ait tüm token'lar silinir ve istek reddedilir.
3. **Given** hırsızlık tespitiyle geçersiz kılınmış bir series, **When** meşru kullanıcı (artık sahipsiz kalan) cookie'siyle geri döndüğünde, **Then** bilgileriyle yeniden login olması gerekir.
4. **Given** bunun yerine token-based mod aktifse, **When** eski bir cookie geçerlilik süresi içinde tekrar oynatıldığında, **Then** kabul edilir — token-based modun hırsızlığı tespit edemediğini gösterir.

---

**User Story 5 – Expired Token Cleanup (Priority: P3)**

Zamanlanmış bir arka plan temizleyicisi, `persistent_logins` tablosundan süresi dolmuş satırları siler; tablo sonsuza kadar büyümez. Token Inspector sayfası silinmeleri anlık olarak yansıtır.

**Why this priority:** Operasyonel hijyen; değerli ama uygulama onsuz da çalışır.

**Independent Test:** Kısa bir token geçerlilik süresi ve temizleyici aralığı ayarlanıp, birkaç token oluşturularak, süre dolduktan sonra satırların Token Inspector / MySQL'den kaybolduğu izlenerek test edilebilir.

**Acceptance Scenarios:**

1. **Given** geçerlilik süresini aşmış token'lar var, **When** zamanlanmış temizleyici çalıştığında, **Then** yalnızca süresi dolmuş satırlar silinir.
2. **Given** geçerlilik süresi içinde olan token'lar var, **When** temizleyici çalıştığında, **Then** bu satırlara dokunulmaz.
3. **Given** repository geçici olarak erişilemez durumda, **When** temizleyici çalıştığında, **Then** hata loglanır ve uygulama istekleri karşılamaya devam eder.

---

**User Story 6 – Custom Cookie & Parameter Names (Priority: P3)**

Uygulama, varsayılan `remember-me` cookie ve HTTP parametre isimlerini kullanmak yerine kendi isimlerini tanımlar (ör. cookie: `notes-rm`, parametre: `keep-me`). Böylece dışarıdan bakan biri, uygulamanın Spring Security remember-me kullandığını cookie isminden hemen anlayamaz. UI'daki cookie inceleme alanı yeni ismi yansıtır.

**Why this priority:** Güvenlik açısından "obscurity" tek başına koruma sağlamaz ama Spring Security'nin özelleştirme API'sini (cookie/parametre isimlendirme) öğretir; temel akışlar onsuz da çalışır.

**Independent Test:** Checkbox işaretli login olup DevTools → Cookies ekranında `remember-me` isimli bir cookie'nin OLMADIĞI, bunun yerine özel isimli cookie'nin set edildiği; ayrıca login isteğinin form verisinde özel parametre adının gittiği doğrulanarak test edilebilir.

**Acceptance Scenarios:**

1. **Given** özel isimler yapılandırılmış durumda, **When** kullanıcı "Remember Me" işaretli login olduğunda, **Then** yanıt varsayılan `remember-me` yerine özel isimli cookie'yi set eder.
2. **Given** login formu, **When** checkbox işaretlenip form gönderildiğinde, **Then** istek varsayılan `remember-me` parametresi yerine özel parametre adını taşır.
3. **Given** özel isimli cookie'ye sahip bir kullanıcı, **When** `JSESSIONID` silinip sayfa yenilendiğinde, **Then** otomatik login yine çalışır (Story 2 davranışı isim değişikliğinden etkilenmez).
4. **Given** eski varsayılan isimle (`remember-me`) elle oluşturulmuş bir cookie, **When** session'sız bir istek geldiğinde, **Then** cookie yok sayılır ve otomatik login gerçekleşmez.

---

**User Story 7 – IP-Bound Remember-Me (Priority: P3)**

Remember-me token'ı, oluşturulduğu istemcinin IP adresine bağlanır: otomatik login yalnızca token'ın üretildiği IP'den gelen isteklerde kabul edilir. Farklı bir IP'den tekrar oynatılan cookie — geçerli olsa bile — reddedilir. UI, token'ın hangi IP'ye bağlı olduğunu gösterir; böylece öğrenen, Story 4'teki hırsızlık senaryosunun bir katman daha nasıl sertleştirildiğini görür.

**Why this priority:** İleri seviye bir özelleştirme alıştırmasıdır (custom remember-me services); temel akışlara bağımlıdır ve onlar olmadan anlamı yoktur.

**Independent Test:** Remember-me ile login olup `JSESSIONID` silindiğinde aynı makineden otomatik login'in çalıştığı; ardından isteğin IP'si değiştirilerek (ör. farklı bir ağ arayüzü/proxy üzerinden veya test isteğinde `X-Forwarded-For` ile) aynı cookie'nin reddedildiği doğrulanarak test edilebilir.

**Acceptance Scenarios:**

1. **Given** remember-me ile login olmuş bir kullanıcı, **When** aynı IP'den session'sız bir istek geldiğinde, **Then** otomatik login kabul edilir.
2. **Given** geçerli bir remember-me cookie'si, **When** token'ın bağlı olduğu IP'den FARKLI bir IP'den istek geldiğinde, **Then** otomatik login reddedilir ve login sayfası gösterilir.
3. **Given** farklı IP'den reddedilmiş bir istek, **When** kullanıcı bilgileriyle yeniden login olduğunda, **Then** yeni IP'ye bağlı yeni bir token üretilir ve o IP'den otomatik login tekrar çalışır.
4. **Given** IP bağlama özelliği kapalıyken üretilmiş bir token, **When** özellik açıldıktan sonra kullanıldığında, **Then** token geçersiz sayılır ve yeniden login gerekir.
