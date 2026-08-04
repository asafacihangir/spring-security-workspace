# Spring Security remember me services öğren

> **Claude Code** — 30 Tem 11:31

---

1. [/cio:learn-from-doc:teach-from-zero @spring-security/raw/ch07-remember_me_servic](#t1)
2. [/cio:practice-exercises @spring-security/raw/ch07-remember_me_services.pdf](#t2)

---

<a id="t1"></a>

## 1 · /cio:learn-from-doc:teach-from-zero @spring-security/raw/ch07-remember_me_servic [↑](#top)

I'll load the teaching skill first, then read the PDF.

# Bölüm 7 — Remember-Me Services (Beni Hatırla) | Sıfırdan Rehberli Ders

Bu dersi hiç bilmediğini varsayarak yazıyorum. En temel sorudan başlayıp, bölümün en ileri konusu olan "IP adresine bağlı özel remember-me" implementasyonuna kadar adım adım ilerleyeceğiz.

---

## SEVİYE 0 — Önce Temel Kavramlar

Remember-me'yi anlamak için önce 3 şeyi bilmen gerekiyor. Bunlar bilinmeden bölümün geri kalanı havada kalır.

### 0.1 HTTP "stateless"tir — yani unutkandır

Web'in çalışma protokolü olan HTTP, doğası gereği **hafızasızdır**. Her istek (request) birbirinden bağımsızdır. Sunucu, sana bir saniye önce gelen isteğin kim olduğunu hatırlamaz.

> **Analoji:** Her seferinde kapıyı çalan ama seni hiç tanımayan bir kuryeyi düşün. Dün 40 kez geldi ama bugün yine "Siz kimsiniz?" diye soruyor.

### 0.2 Cookie (çerez) nedir?

Cookie, sunucunun tarayıcıya "şunu sakla, bir dahaki sefere bana geri getir" diye verdiği küçük bir metin parçasıdır. Tarayıcı bunu diske/hafızaya yazar ve aynı siteye giden her istekte otomatik olarak geri gönderir.

> **Analoji:** Kuryeye bir kimlik kartı verirsin. Bir dahaki gelişinde kartı gösterir, sen de "Ha, tamam, sen o kişisin" dersin.

Kitap burada Wikipedia'nın HTTP cookie sayfasını referans veriyor.

### 0.3 Session (oturum) nedir ve JSESSIONID

Sunucu, giriş yapan kullanıcıyı hatırlamak için hafızasında bir "oturum" tutar ve bu oturumun kimlik numarasını `JSESSIONID` adlı bir cookie ile tarayıcıya verir.

Ama `JSESSIONID`'nin **iki büyük sınırı** vardır:

| Durum | Sonuç |
|---|---|
| Tarayıcı kapanır | Cookie silinir → oturum biter |
| Belirli süre işlem yapılmaz | Oturum sunucuda expire olur (zaman aşımı) |

Yani tarayıcıyı kapattığın anda tekrar login olman gerekir.

**İşte remember-me tam olarak bu sorunu çözer.**

---

## SEVİYE 1 — Remember-Me Nedir?

**Tanım:** Kullanıcının oturumu bitse ve tarayıcı kapansa bile uygulamanın onu hatırlamasını sağlayan özelliktir.

Spring Security bunu **ayrı bir cookie** ile yapar: `remember-me` cookie'si.

Akış çok basit:

1. Kullanıcı login formunda "Beni Hatırla" kutusunu işaretler.
2. Spring Security tarayıcıya `remember-me` cookie'si yazar.
3. Kullanıcı tarayıcıyı kapatıp açar, siteye girer.
4. Spring Security `remember-me` cookie'sini görür, doğrular, kullanıcıyı **otomatik olarak** login eder.
5. Kullanıcı hiç kullanıcı adı/şifre girmez.

> **Kritik nokta:** `JSESSIONID` ve `remember-me` **iki farklı cookie**'dir. İsimleri benziyor, işleri farklı. `JSESSIONID` = "şu an açık oturum". `remember-me` = "oturum yoksa bile yeniden oturum aç".

### Spring Security'nin iki stratejisi

Bölüm boyunca iki yöntem işleniyor. Şimdiden aklında bulunsun, çünkü tüm bölüm bu ikisi arasındaki farkı anlatıyor:

| Strateji | Nasıl doğrular? | Veritabanı gerekir mi? |
|---|---|---|
| **Token-based** | Kriptografik imza (signature) ile | Hayır |
| **Persistent-based** | Veritabanındaki kayıtla karşılaştırarak | Evet |

Remember-me **varsayılan olarak kapalıdır**, açıkça yapılandırman gerekir.

---

## SEVİYE 2 — Token-Based Remember-Me (En Basit Yöntem)

### 2.1 Bağımlılıklar

Token-based için **ekstra bağımlılık gerekmez**. Bölüm 2'deki temel Spring Security kurulumu yeterlidir.

Ama ilerideki persistent-based bölümü için `build.gradle` dosyasına şunlar lazım (kitabın örnek kodunda zaten var):

```gradle
//build.gradle
dependencies {
...
    // JPA / ORM / Hibernate:
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    // H2 db
    implementation 'com.h2database:h2'
...
}
```

### 2.2 Adım 1 — Konfigürasyonu aç

`SecurityConfig.java` dosyasına `rememberMe` metodunu ekliyoruz:

```java
//src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
...
    // Remember Me
    http.rememberMe(httpSecurityRememberMeConfigurer ->
        httpSecurityRememberMeConfigurer.key("jbcpCalendar"));
...
}
```

Buradaki `key("jbcpCalendar")` şimdilik "gizli anahtar" olarak aklında kalsın — birazdan neden hayati olduğunu göreceğiz.

> Kitap notu: Kaynak kod olarak `chapter07.00-calendar` ile başlaman gerekiyor.

### 2.3 Adım 2 — Login formuna checkbox ekle

Sadece konfigürasyonu açmak yetmez. Uygulamayı çalıştırsan **hiçbir şey değişmez**. Çünkü kullanıcının bu özelliği seçebileceği bir alan yok.

`login.html` dosyasına checkbox ekliyoruz:

```html
//src/main/resources/templates/login.html
<div class="mb-3">
    <label class="form-label" for="password">Password</label>
    <input class="form-control" id="password" name="password"
           type="password"/>
</div>

<div class="mb-3">
    <label for="remember-me">Remember Me?</label>
    <input type="checkbox" id="remember-me" name="remember-me"
           th:checked="true" />
</div>

<div class="mb-3">
    <input class="btn btn-primary" id="submit" name="submit" type="submit"
           value="Login"/>
</div>
```

> **Çok önemli detay:** Checkbox'ın `name` değeri **`remember-me`** olmalı. Spring Security bu HTTP parametresine bakarak "kullanıcı hatırlanmak istiyor" kararını verir.
>
> Spring Security 4.x ve sonrasında varsayılan form alanı adı `remember-me`'dir. Değiştirmek istersen `rememberMeParameter` metodunu kullanırsın.

> Kaynak kod: `chapter07.01-calendar`

### 2.4 Adım 3 — Test et

1. Remember-me kutusu işaretli olarak login ol.
2. Ana sayfayı bookmark'la.
3. Tarayıcıyı tamamen kapat, yeniden aç, bookmark'a git.
4. Login sayfası gelmemeli — direkt içeride olmalısın.

**Test ipuçları (kitaptan):**
- Önce sekmeyi kapat, sonra tarayıcıyı kapat.
- Eğer hâlâ giriş yapmış görünüyorsan, muhtemelen tarayıcın veya bir eklenti oturumu geri yüklüyordur.
- **En pratik yol:** Chrome Developer Tools → Application → Cookies → `JSESSIONID` cookie'sini sil. Bu, oturumu öldürür ama `remember-me` cookie'sini bırakır. Geliştirme sırasında çok zaman kazandırır.

DevTools'ta login sonrası **iki cookie** görmelisin: `JSESSIONID` ve `remember-me`.

---

## SEVİYE 3 — Token-Based Nasıl Çalışıyor? (İçini Açalım)

Şimdi kritik kısım: O `remember-me` cookie'sinin içinde ne var?

### 3.1 Cookie'nin içeriği

Cookie, Base64 ile kodlanmış bir metindir ve içinde şunlar bulunur:

1. **Kullanıcı adı** (username)
2. **Son kullanma tarihi/saati** (expiration)
3. **SHA-256 hash değeri** — şunlardan üretilir: expiration + username + password + `key`

Cookie'nin oluşum formülü şudur:

```
base64(username + ":" + expirationTime + ":" + algorithmName + ":"
algorithmHex(username + ":" + expirationTime + ":" password + ":" + key))
```

| Parça | Anlamı |
|---|---|
| `username` | `UserDetailsService` tarafından tanınabilir kullanıcı adı |
| `password` | Alınan `UserDetails`'teki şifreyle eşleşen değer |
| `expirationTime` | Token'ın geçerliliğinin biteceği an (milisaniye) |
| `key` | Token'ın değiştirilmesini engelleyen gizli anahtar |
| `algorithmName` | Token'ı üretmek ve doğrulamak için kullanılan algoritma |

> **Dikkat:** Şifre cookie'ye **yazılmaz**. Şifre sadece hash hesabına girer. Hash'ten şifreyi geri elde etmek pratikte imkânsızdır.

### 3.2 SHA-256 Algoritması

Varsayılan olarak imza SHA-256 ile üretilir. Doğrulamada, cookie içindeki `algorithmName` okunur ve o algoritma kullanılır. `algorithmName` yoksa varsayılan olarak SHA-256 kabul edilir.

Bu tasarımın güzelliği: **eski cookie'leri kırmadan algoritma yükseltebilirsin.** İmzalama için yeni algoritma, doğrulama için eski algoritma tanımlayabilirsin.

Bunun için `TokenBasedRememberMeServices`'i kendin bean olarak tanımlarsın:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http,
RememberMeServices rememberMeServices) throws Exception {
    http
        .authorizeHttpRequests((authorize) -> authorize
            .anyRequest().authenticated()
        )
        .rememberMe((remember) -> remember
            .rememberMeServices(rememberMeServices)
        );
    return http.build();
}

@Bean
RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
    RememberMeTokenAlgorithm encodingAlgorithm = RememberMeTokenAlgorithm.SHA256;
    TokenBasedRememberMeServices rememberMe = new
        TokenBasedRememberMeServices(myKey, userDetailsService, encodingAlgorithm);
    rememberMe.setMatchingAlgorithm(RememberMeTokenAlgorithm.MD5);
    return rememberMe;
}
```

Burada: **yeni token'lar SHA-256 ile üretilir**, ama **eski MD5 token'lar hâlâ doğrulanabilir**. Kademeli geçiş (safe upgrade) tam olarak budur.

### 3.3 Remember-me imzası (signature) — doğrulama mantığı

Bu bölümün kalbi burası. Mantık şu:

> **Cookie'ye güvenilmez. Ama imzasını doğrulayabilirsek, onun bizim uygulamamızdan çıktığına güvenebiliriz.**

Doğrulama adımları (Figure 7.2'nin anlattığı akış):

1. Cookie'den **username** ve **expiration** çıkarılır.
2. Username ile `UserDetailsService`'e gidilir, **password** alınır.
3. **key** zaten bilinir (konfigürasyonda tanımladık).
4. Bu 4 değerden (**username + expiration + password + key**) **beklenen imza** (expected signature) hesaplanır.
5. Beklenen imza ile cookie'deki imza karşılaştırılır.

**Sonuç:**
- İmzalar eşleşiyorsa → username ve expiration'a güvenilir → token süresi de dolmamışsa kullanıcı login edilir.
- Eşleşmiyorsa → cookie sahte veya bozulmuş.

**Neden sahte imza üretmek imkânsıza yakın?**
Saldırganın iki şeyi birden bilmesi gerekir:
- `key` → sadece uygulama bilir
- kullanıcının şifresi → sadece kullanıcı bilir

### 3.4 Önemli yan etki: Şifre değişirse token ölür

İmza hesabına şifre girdiği için, **kullanıcı şifresini veya kullanıcı adını değiştirdiğinde tüm remember-me token'ları geçersiz olur.**

Kitabın uyarısı: Kullanıcılara bu bilgiyi uygun bir mesajla ver. (İleride, sadece username'e dayanan, şifreye bağımlı olmayan bir alternatif göreceğiz — persistent-based.)

### 3.5 Yapılandırma direktifleri

| Attribute | Açıklama |
|---|---|
| `key` | Remember-me cookie'sinin imzasını üretirken kullanılan benzersiz anahtar |
| `tokenValiditySeconds` | Cookie'nin kaç saniye geçerli sayılacağı. Aynı zamanda cookie'nin expiration timestamp'ini de belirler |

**`key` hakkında (güvenlik açısından kritik):**

Kitapta örnekler basit tutulmuş (`jbcpCalendar`), ama gerçek uygulamada:
- Uygulamanın benzersiz adını içermeli
- **En az 36 rastgele karakter** olmalı
- Alfanumerik + özel karakter karışımı olmalı (online password generator kullanılabilir)
- **Ortam bilgisini (dev/test/prod) içermeli** → böylece test sırasında bir ortamın cookie'si yanlışlıkla başka ortamda kullanılamaz

Prod örneği:

```
prodJbcpCalendar-rmkey- YWRtaW4xJTQwZXhhbXBsZS5jb20lM0ElMkY...
```

**`tokenValiditySeconds` hakkında:**

- Belirtilen saniye sonrası token otomatik login için kabul edilmez (geçerli görünse bile).
- `-1` verilirse: cookie bir **session cookie**'ye dönüşür, tarayıcı kapanınca silinir. Token, tarayıcı açık kaldığı sürece **2 hafta** geçerlidir (bu süre ayarlanamaz).

---

## SEVİYE 4 — Remember-Me Güvenli mi?

Kısa cevap: **Kolaylık her zaman bir güvenlik riski taşır.**

### 4.1 Replay Attack (tekrar saldırısı)

Figure 7.3'ün anlattığı senaryo:

1. Kullanıcı, `remember-me` cookie'si ile istek yapar.
2. Kötü niyetli kişi, ağ dinleyicisi (network sniffer) ile bu isteği **kaydeder**.
3. Aynı isteği **tekrar oynatır** (replay).
4. Sunucu cookie'yi geçerli görür → saldırgan kullanıcı olarak içeri girer.

> **Analoji:** Cookie, kapıyı açan bir anahtar kartıdır. Kartını kopyalayan herkes içeri girer — kartın gerçek sahibi olup olmadığına bakılmaz.

### 4.2 Karşı önlemler

| Önlem | Ne sağlar |
|---|---|
| **SSL/TLS** | Trafiği şifreler, sniffer cookie'yi okuyamaz |
| **XSS'e karşı koruma** | Cross-Site Scripting ile cookie çalınmasını engeller |
| **`fullyAuthenticated` kuralları** | Hassas sayfaları remember-me ile korumasız bırakmaz |

Kitap OWASP Top Ten'i (https://owasp.org/www-project-top-ten/) okumanızı öneriyor.

### 4.3 Yetkilendirme kuralları: `fullyAuthenticated`

En pratik denge şudur: **Hassas bölgeler için tam kimlik doğrulama iste.**

Spring Security, kullanıcının nasıl giriş yaptığını ayırt edebilir:
- **Remembered** → cookie ile otomatik giriş yapmış
- **Fully authenticated** → gerçekten kullanıcı adı + şifre girmiş

> **Gerçek hayat örneği:** E-ticaret siteleri seni hatırlar, ürünlere bakabilirsin. Ama kredi kartı bilgilerini görmek istediğinde şifre sorar. İşte `fullyAuthenticated` budur.

Örnek uygulamada H2 veritabanı konsolunu koruyoruz:

```java
//src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
...
@Bean
public SecurityFilterChain filterChain(HttpSecurity http,
PersistentTokenRepository persistentTokenRepository) throws Exception {

    http.authorizeHttpRequests( authz -> authz
            .requestMatchers("/webjars/**").permitAll()
            .requestMatchers("/css/**").permitAll()
            .requestMatchers("/favicon.ico").permitAll()

            // H2 console:
            .requestMatchers("/admin/h2/**")
            .access(new
WebExpressionAuthorizationManager("isFullyAuthenticated() and
hasRole('ADMIN')"))
...
    // Remember Me
    http.rememberMe(httpSecurityRememberMeConfigurer ->
httpSecurityRememberMeConfigurer
        .key("jbcpCalendar").
tokenRepository(persistentTokenRepository));
...
}
```

Buradaki kural iki şart birden ister: kullanıcı **hem** `ROLE_ADMIN` yetkisine sahip olacak, **hem de** tam kimlik doğrulamasından geçmiş olacak.

> **Not:** `and`, `or`, `not` ifadeleri **SpEL** (Spring Expression Language) mantıksal operatörleridir. SpEL tasarımcıları bunu düşünceli davranarak eklemiş — çünkü `&&` operatörünü XML içinde yazmak zahmetlidir (buradaki örnek Java tabanlı olsa da).

**Test senaryosu:**
1. `admin1@example.com` / `admin1` ile, remember-me işaretli login ol.
2. H2 konsoluna eriş → **başarılı**.
3. `JSESSIONID` cookie'sini sil (veya sekmeyi ve tüm tarayıcı pencerelerini kapat).
4. **All Events** sayfasına eriş → **hâlâ başarılı** (remember-me çalışıyor).
5. H2 konsoluna eriş → **erişim reddedildi** (çünkü artık sadece "remembered"sın, "fully authenticated" değilsin).

> Kaynak kod: `chapter07.02-calendar`

---

## SEVİYE 5 — Persistent Remember-Me (Veritabanı Destekli)

Şimdi ikinci stratejiye geçiyoruz. `RememberMeServices` arayüzünün farklı bir implementasyonunu kullanarak doğrulama yöntemini tamamen değiştiriyoruz.

**Temel fark:**
- Token-based: "İmza doğru mu?" diye **hesaplar**.
- Persistent-based: "Bu token veritabanında var mı?" diye **bakar**.

### 5.1 Veritabanı şeması

```sql
//src/main/resources/schema.sql
create table persistent_logins
(
    username  varchar_ignorecase(50) not null,
    series    varchar(64) primary key,
    token     varchar(64) not null,
    last_used timestamp   not null
);
```

### 5.2 Veri kaynağını hazırlama

Spring Data, gömülü veritabanını `schema.sql` ile otomatik başlatır. Ancak **JPA** kullanıldığında, şemanın oluşması ve `data.sql`'in çalışması için data source başlatmasının **ertelenmesi** gerekir:

```yaml
//src/main/resources/application.yml
spring:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    show-sql: false
    hibernate:
      ddl-auto: create-drop
    defer-datasource-initialization: true
```

### 5.3 Konfigürasyon

Değişiklik şaşırtıcı derecede küçük — `rememberMe` içine `tokenRepository` eklemek yeterli. Spring Security bunu görür ve `RememberMeServices` implementasyonunu otomatik olarak değiştirir:

```java
//src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http,
PersistentTokenRepository persistentTokenRepository) throws Exception {
    http.authorizeRequests( authz -> authz
    ...
    // Remember Me
    http.rememberMe(httpSecurityRememberMeConfigurer ->
httpSecurityRememberMeConfigurer
            .key("jbcpCalendar").
tokenRepository(persistentTokenRepository));
    return http.build();
}

@Bean
public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
    JdbcTokenRepositoryImpl db = new JdbcTokenRepositoryImpl();
    db.setDataSource(dataSource);
    return db;
}
```

> Kullanıcı açısından **hiçbir fark yoktur**. Ama arka plandaki mekanizma tamamen değişmiştir.
>
> Kaynak kod: `chapter07.03-calendar`

### 5.4 Persistent-based nasıl çalışır?

Cookie artık imza taşımaz. İki değer taşır:

| Parça | Anlamı |
|---|---|
| **Series identifier** | Kullanıcının **ilk login**'ini tanımlar. Aynı otomatik-login zinciri boyunca **değişmez** |
| **Token value** | Her otomatik login'de **değişen** benzersiz değer |

Doğrulama akışı (Figure 7.4):

1. Cookie gelir, içindeki **series identifier** okunur.
2. `PersistentTokenRepository`'den bu series'e ait **beklenen token** ve **expiration** çekilir.
3. Cookie'deki token ile beklenen token karşılaştırılır.
4. **Eşleşiyor ve süresi dolmamışsa** → kullanıcı authenticated sayılır. Ardından **aynı series**, **yeni bir token değeri** ve **güncellenmiş expiration** ile yeni bir cookie üretilir.
5. **Series var ama token eşleşmiyorsa** → cookie **çalınmış** kabul edilir. Spring Security o series'e ait tüm token'ları **iptal eder** ve kullanıcıyı uyarır.

> **Analoji (bu mekanizmanın dehası):** Otel odası anahtar kartı gibi. Her odaya girişinde kart yeniden kodlanır. Biri kartını kopyaladıysa, ikinizden biri kartı kullandığı anda diğerinin kartı geçersizleşir — ve otel "birisi kart kopyalamış" diye anlar.

Kalıcı token'ları H2 konsolunda görebilirsin:

```sql
SELECT * FROM PERSISTENT_LOGINS;
```

| LAST_USED | SERIES | TOKEN | USERNAME |
|---|---|---|---|
| 2023-10-17 18:27:10.19 | NI8Kolo6IvuLueS+UEkzmw== | vA1adzbRHYXu5rCFNIRQw== | admin1@example.com |

---

## SEVİYE 6 — JPA Tabanlı PersistentTokenRepository

`JdbcTokenRepositoryImpl` JDBC kullanır. Projenin geri kalanı Spring Data JPA kullandığı için, tutarlılık adına bunu JPA'ya çeviriyoruz.

### Adım 1 — Domain nesnesi

```java
//src/main/java/com/packtpub/springsecurity/domain/PersistentLogin.java
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin implements Serializable {

    @Id
    private String series;

    private String username;
    private String token;
    private Date lastUsed;

    public PersistentLogin(){}

    public PersistentLogin(PersistentRememberMeToken token){
        this.series = token.getSeries();
        this.username = token.getUsername();
        this.token = token.getTokenValue();
        this.lastUsed = token.getDate();
    }

// getters/setters omitted for brevity
}
```

### Adım 2 — JPA Repository

```java
//src/main/java/com/packtpub/springsecurity/repository/RememberMeTokenRepository.java
import java.util.Date;
import java.util.List;

import com.packtpub.springsecurity.domain.PersistentLogin;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RememberMeTokenRepository extends
JpaRepository<PersistentLogin, String> {

    PersistentLogin findBySeries(String series);
    List<PersistentLogin> findByUsername(String username);
    Iterable<PersistentLogin> findByLastUsedAfter(Date expiration);

}
```

### Adım 3 — Özel PersistentTokenRepository

Dört metodu override etmemiz gerekiyor. Bunlar `PersistentTokenRepository` arayüzünün sözleşmesidir:

```java
//src/main/java/com/packtpub/springsecurity/web/authentication/rememberme/JpaPersistentTokenRepository.java:
public class JpaPersistentTokenRepository implements PersistentTokenRepository {

    private final RememberMeTokenRepository rememberMeTokenRepository;

    public JpaPersistentTokenRepository(RememberMeTokenRepository rememberMeTokenRepository) {
        this.rememberMeTokenRepository = rememberMeTokenRepository;
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogin newToken = new PersistentLogin(token);
        this.rememberMeTokenRepository.save(newToken);
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        PersistentLogin token = this.rememberMeTokenRepository.findBySeries(series);
        if (token != null) {
            token.setToken(tokenValue);
            token.setLastUsed(lastUsed);
            this.rememberMeTokenRepository.save(token);
        }
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        PersistentLogin token = this.rememberMeTokenRepository.findBySeries(seriesId);
        if(token == null){
            return null;
        } else {
            return new PersistentRememberMeToken(token.getUsername(),
                    token.getSeries(),
                    token.getToken(),
                    token.getLastUsed());
        }
    }

    @Override
    public void removeUserTokens(String username) {
        List<PersistentLogin> tokens = this.rememberMeTokenRepository.findByUsername(username);
        this.rememberMeTokenRepository.deleteAll(tokens);
    }

}
```

Bu dört metodun anlamı, tam olarak Seviye 5.4'teki akışın kod karşılığıdır:

| Metod | Ne zaman çağrılır |
|---|---|
| `createNewToken` | Kullanıcı ilk kez remember-me ile login olduğunda |
| `updateToken` | Her otomatik login'de (token değeri yenilenir) |
| `getTokenForSeries` | Cookie doğrulanırken beklenen token aranırken |
| `removeUserTokens` | Logout veya token iptali gerektiğinde |

### Adım 4 — Bean tanımını değiştir

```java
/src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
@Bean
public PersistentTokenRepository persistentTokenRepository(
        RememberMeTokenRepository rmtr) {
    return new JpaPersistentTokenRepository(rmtr);
}
```

Konfigürasyonun geri kalanı değişmez. Kullanıcı yine hiçbir fark hissetmez.

> Kaynak kod: `chapter07.04-calendar`

---

## SEVİYE 7 — Custom RememberMeServices

Şimdiye kadar `PersistentTokenRepository`'nin oldukça basit implementasyonlarını kullandık. Bu, cookie kalıcılığı üzerinde **sınırlı kontrol** verir.

Daha fazla kontrol istiyorsak, `PersistentTokenRepository`'yi bir `RememberMeServices` implementasyonunun içine **sarmalarız** (wrap). Spring Security'nin bu amaçla hazır bir sınıfı vardır: **`PersistentTokenBasedRememberMeServices`**.

```java
//src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http,
        PersistentTokenRepository persistentTokenRepository,
RememberMeServices rememberMeServices) throws Exception {
     http.authorizeHttpRequests( authz -> authz
                    .requestMatchers("/webjars/**").permitAll()
…
     // Remember Me
     http.rememberMe(httpSecurityRememberMeConfigurer ->
httpSecurityRememberMeConfigurer
            .key("jbcpCalendar")
            .rememberMeServices(rememberMeServices)
            .tokenRepository(persistentTokenRepository));

     return http.build();
}
@Bean
public RememberMeServices rememberMeServices
(PersistentTokenRepository ptr,
UserDetailsService  userDetailsService){
    PersistentTokenBasedRememberMeServices rememberMeServices = new
            PersistentTokenBasedRememberMeServices("jbcpCalendar",
            userDetailsService, ptr);
    rememberMeServices.setAlwaysRemember(true);
    return rememberMeServices;
}
```

`setAlwaysRemember(true)` → checkbox işaretlenmiş olsun ya da olmasın, **her zaman** remember-me uygular.

> Kaynak kod: `chapter07.05-calendar`

### Veritabanı destekli token'lar daha mı güvenli?

Dürüst cevap: **Cookie hırsızlığına karşı ikisi de savunmasızdır.** `TokenBasedRememberMeServices` gibi, persistent token'lar da cookie çalınması veya man-in-the-middle saldırılarıyla ele geçirilebilir. SSL kullanımı bunu büyük ölçüde engeller.

**Servlet 5.0 ortamında (yani Tomcat 10+)**, Spring Security cookie'yi `HttpOnly` olarak işaretler. Bu, XSS açığı olsa bile JavaScript'in cookie'yi okumasını engeller.

**Persistent-based'in gerçek avantajı şudur:**

> Cookie'nin ele geçirildiğini **tespit edebiliriz.**

Doğru series ama yanlış token gelirse, o series'in kompromize olduğunu **kesin olarak biliriz** ve ilgili tüm oturumları sonlandırabiliriz. Doğrulama **stateful** (durumlu) olduğu için, **kullanıcının şifresini değiştirmeden** o remember-me özelliğini iptal edebiliriz.

Token-based'de bu mümkün değildir — orada token'ı iptal etmenin tek yolu şifreyi değiştirmektir.

---

## SEVİYE 8 — Süresi Dolmuş Token'ları Temizleme

Persistent-based'in **dezavantajı**: süresi dolmuş oturumları temizleyen **yerleşik bir mekanizma yoktur.** Veritabanı zamanla çöp token'larla dolar.

Çözüm: arka planda çalışan bir temizleyici yazmak.

```java
//src/main/java/com/packtpub/springsecurity/web/authentication/rememberme/ JpaTokenRepositoryCleaner.java
public class JpaTokenRepositoryCleaner implements Runnable {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final RememberMeTokenRepository rememberMeTokenRepository;

    private final long tokenValidityInMs;

    public JpaTokenRepositoryCleaner(RememberMeTokenRepository rememberMeTokenRepository,
                                     long tokenValidityInMs) {
        if (rememberMeTokenRepository == null) {
            throw new IllegalArgumentException("jdbcOperations cannot be null");
        }
        if (tokenValidityInMs < 1) {
            throw new IllegalArgumentException("tokenValidityInMs must be greater than 0. Got " + tokenValidityInMs);
        }
        this.rememberMeTokenRepository = rememberMeTokenRepository;
        this.tokenValidityInMs = tokenValidityInMs;
    }

    public void run() {
        long expiredInMs = System.currentTimeMillis() - tokenValidityInMs;

        logger.info("Searching for persistent logins older than {}ms", tokenValidityInMs);

        try {
            Iterable<PersistentLogin> expired =
rememberMeTokenRepository.findByLastUsedAfter(new Date(expiredInMs));
            for(PersistentLogin pl: expired){
                logger.info("**** Removing persistent login for {} ***", pl.getUsername());
                rememberMeTokenRepository.delete(pl);
            }
        } catch(Throwable t) {
            logger.error("**** Could not clean up expired persistent remember me tokens. ***", t);
        }
    }
}
```

> Kitap notu: Yukarıdaki sürüm, kısalık için doğrulama ve hata işleme detaylarından arındırılmıştır; tam sürüm bölümün örnek kodundadır.

Bunu her 10 dakikada bir çalıştıran zamanlayıcı:

```java
//src/main/java/com/packtpub/springsecurity/configuration/ JavaConfig.java@Configuration

@Configuration
@EnableScheduling
public class JavaConfig {

    private RememberMeTokenRepository rememberMeTokenRepository;

    public JavaConfig(RememberMeTokenRepository rememberMeTokenRepository) {
        this.rememberMeTokenRepository = rememberMeTokenRepository;
    }

    @Scheduled(fixedRate = 600_000)
    public void tokenRepositoryCleaner(){
        Thread trct = new Thread(
                new JpaTokenRepositoryCleaner(
                        rememberMeTokenRepository,
                        100_000L));
        trct.start();
    }

}
```

> **Üretim uyarısı (kitaptan):** Bu konfigürasyon **cluster-aware değildir.** Uygulama bir cluster'a deploy edilirse, temizleyici her JVM'de **ayrı ayrı** çalışır.

Denemek için `@Scheduled` değerini düşür, birkaç token oluştur ve H2 konsolundan silinişlerini izle.

> Kaynak kod: `chapter07.06-calendar`

---

## SEVİYE 9 — Remember-Me Mimarisi (Parçalar Nasıl Birleşiyor?)

Şimdi tüm resmi görelim. Figure 7.6'nın anlattığı akış:

```
Kullanıcı İsteği
      ↓
RememberMeAuthenticationFilter tarafından yakalanır
      ↓
RememberMeServices ile istek incelenir
   (TokenBasedRememberMeServices bu arayüzü implement eder)
      ↓
Cookie var mı?
   ├─ Hayır → zincirdeki sonraki filtreyi çağır
   └─ Evet ↓
Cookie decode edilebildi mi?
   ├─ Hayır → InvalidCookieException fırlat
   └─ Evet ↓
Cookie imzası ile Beklenen imza karşılaştırılır
      ↓
İmzalar eşleşti mi?
   ├─ Hayır → Exception
   └─ Evet ↓
Kullanıcı hesabı uygun mu?
   ├─ Hayır → java.lang.Exception fırlat
   └─ Evet ↓
RememberMeAuthenticationToken oluşturulur
      ↓
Kullanıcı Authenticated
```

**Sözle özeti:**

Tüm Spring Security filtreleri gibi, `RememberMeAuthenticationFilter` da `FilterChainProxy` içinden çağrılır. Görevi isteği incelemek ve ilgi çekici bir şey varsa aksiyon almaktır. Bunu yapmak için `RememberMeServices` implementasyonunu kullanır; o da HTTP isteğinde remember-me cookie'si arar ve token-based veya persistent-based doğrulamayı uygular. Token doğrulanırsa kullanıcı login edilir.

### 9.1 Remember-Me ve kullanıcı yaşam döngüsü

`RememberMeServices`, kullanıcının oturum yaşam döngüsünün birkaç noktasında devreye girer:

| Aksiyon | Ne olmalı? | Çağrılan metod |
|---|---|---|
| **Başarılı login** | Form parametresi gönderilmişse cookie oluşturulur | `loginSuccess` |
| **Başarısız login** | Varsa cookie iptal edilir | `loginFailed` |
| **Kullanıcı logout** | Varsa cookie iptal edilir | `Logout` |

> **Önemli teknik detay:** `logout` metodu aslında `RememberMeServices` arayüzünde **yoktur.** Her `RememberMeServices` implementasyonu ayrıca `LogoutHandler` arayüzünü de implement eder — `logout` metodu oradan gelir. Böylece kullanıcı çıkış yaptığında gerekli temizlik yapılabilir.

Bu bilgi, ileride **özel authentication handler** yazarken kritik olacak: yazdığın her authentication processor'ın `RememberMeServices`'i tutarlı biçimde ele alması gerekir, aksi halde bu özelliğin hem faydası hem güvenliği bozulur.

---

## SEVİYE 10 — İleri Seviye: Remember-Me'yi IP Adresine Bağlamak

Şimdi öğrendiklerimizi kullanarak gerçek bir güvenlik iyileştirmesi yapalım.

**Hedef:** Remember-me token'ı, onu oluşturan kullanıcının **IP adresine bağlansın**. Çalınan bir cookie, farklı bir IP'den kullanılamasın.

**Yöntem:** Özel bir `PersistentTokenRepository` yazmak. Bölümün örnek kodundaki sınıf: `IpAwarePersistentTokenRepository`.

**Temel fikri (püf noktası budur):**

> Series identifier, **içeride** (veritabanında) IP adresiyle birleştirilerek saklanır. **Dışarıya** (cookie'ye) ise sadece saf series identifier verilir.

Yani token her aranışında veya kaydedilişinde, **o anki IP adresi** hesaba katılır.

### 10.1 IP adresini almak

Spring Security'nin `RequestContextHolder` sınıfı kullanılır:

```java
//src/main/java/com/packtpub/springsecurity/web/authentication/rememberme/ IpAwarePersistentTokenRepository.java
private String ipSeries(String series) {
    ServletRequestAttributes attributes = (ServletRequestAttributes)
RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
        throw new IllegalStateException("RequestContextHolder.getRequestAttributes() cannot be null");
    }
    String remoteAddr = attributes.getRequest().getRemoteAddr();
    logger.debug("Remote address is {}", remoteAddr);

    return series + remoteAddr;
}
```

> **Kurulum notu:** `RequestContextHolder` kullanmak için `web.xml` dosyanda `RequestContextListener` tanımlı olmalı. Örnek kodda bu ayar zaten yapılmış. Harici bir uygulamada kullanacaksan `IpAwarePersistentTokenRepository`'nin Javadoc'una bak.

### 10.2 Token kaydederken IP'yi ekle

```java
@Override
public void createNewToken(PersistentRememberMeToken token) {
    String ipSeries = ipSeries(token.getSeries());
    PersistentRememberMeToken ipToken = tokenWithSeries(token, ipSeries);
    this.delegateRepository.createNewToken(ipToken);
}
```

Önce IP eklenmiş yeni bir series oluşturulur. `tokenWithSeries` metodu, diğer tüm değerleri aynı bırakıp sadece series'i değiştiren bir yardımcıdır. Sonuçta oluşan token, **asıl** `PersistentTokenRepository` implementasyonu olan `delegateRepository`'ye gönderilir.

### 10.3 Token ararken IP'yi ekle

```java
@Override
public PersistentRememberMeToken getTokenForSeries(String seriesId) {
    String ipSeries = ipSeries(seriesId);
    PersistentRememberMeToken ipToken = delegateRepository.getTokenForSeries(ipSeries);
    return tokenWithSeries(ipToken, seriesId);
}
```

Aranan her token'da mevcut IP series'e eklendiği için, **bir kullanıcının farklı IP'deki bir kullanıcıya ait token'ı elde etmesi imkânsızdır.**

Kodun geri kalanı da benzerdir. Özet mantık:

| Konum | Series identifier'ın hali |
|---|---|
| **İçeride** (veritabanı) | series + IP adresi |
| **Dışarıda** (cookie) | sadece orijinal series |

Bu asimetri sayesinde, **remember-me token'ını sadece onu oluşturan kullanıcı kullanabilir.**

### 10.4 Spring konfigürasyonu

```java
//src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
@Bean
public RememberMeServices rememberMeServices(PersistentTokenRepository ptr,
UserDetailsService userDetailsService) {
    PersistentTokenBasedRememberMeServices rememberMeServices = new
            PersistentTokenBasedRememberMeServices("jbcpCalendar",
            userDetailsService, ptr);
    rememberMeServices.setAlwaysRemember(true);
    return rememberMeServices;
}

@Bean
public IpAwarePersistentTokenRepository
tokenRepository(RememberMeTokenRepository rmtr) {
    return new IpAwarePersistentTokenRepository(new
JpaPersistentTokenRepository(rmtr));
}

@Bean
public OrderedRequestContextFilter requestContextFilter() {
    return new OrderedRequestContextFilter();
}
```

Dikkat et: `IpAwarePersistentTokenRepository`, bir `JpaPersistentTokenRepository`'yi **sarmalıyor** (decorator pattern). Ayrıca `OrderedRequestContextFilter` bean'i, `RequestContextHolder`'ın çalışması için gereken `RequestContextFilter`'ı başlatır.

**Test:** İkinci bir bilgisayardan, Firebug benzeri bir eklentiyle remember-me cookie'sini taşımayı dene. Spring Security artık isteği **yok sayacak** ve ilgili cookie'yi **silecektir.**

> Kaynak kod: `chapter07.07-calendar`

> **Gerçek dünya uyarısı:** IP tabanlı remember-me token'ları, kullanıcı **paylaşımlı veya yük dengeli bir ağ altyapısı** arkasındaysa (örneğin çok noktalı bir kurumsal WAN) beklenmedik davranabilir. Çoğu senaryoda ise bu, faydalı bir özelliğe eklenen değerli bir ek güvenlik katmanıdır.

---

## SEVİYE 11 — Cookie ve HTTP Parametre İsimlerini Gizleme

Son bir ince dokunuş: Spring Security kullandığını belli etmemek için varsayılan isimleri değiştirebilirsin (**security through obscurity**).

### 1. RememberMeServices bean'inde tanımla

```java
//src/main/java/com/packtpub/springsecurity/configuration/SecurityConfig.java
@Bean
public RememberMeServices rememberMeServices(PersistentTokenRepository ptr,
UserDetailsService userDetailsService) {
    PersistentTokenBasedRememberMeServices rememberMeServices = new
            PersistentTokenBasedRememberMeServices("jbcpCalendar",
            userDetailsService, ptr);
    rememberMeServices.setAlwaysRemember(true);
    rememberMeServices.setParameter("obscure-remember-me");
    rememberMeServices.setCookieName("obscure-remember-me");
    return rememberMeServices;
}
```

| Metod | Neyi değiştirir |
|---|---|
| `setParameter(...)` | Formdaki HTTP parametre adı |
| `setCookieName(...)` | Tarayıcıdaki cookie adı |

### 2. login.html'i unutma!

```html
//src/main/resources/templates/login.html
<div class="mb-3">
    <label for="remember-me">Remember Me?</label>
    <input type="checkbox" id="remember-me" name="obscure-remember-me" th:checked="true" />
</div>
```

> **En sık yapılan hata burasıdır.** `setParameter` ile checkbox'ın `name` değeri **birebir aynı** olmalıdır. Aksi halde Spring Security kullanıcının kutuyu işaretlediğini hiç fark etmez ve özellik sessizce çalışmaz.

> Kaynak kod: `chapter07.08-calendar`

---

## GENEL ÖZET — Tek Bakışta Bölüm

### İki stratejinin karşılaştırması

| Kriter | Token-Based | Persistent-Based |
|---|---|---|
| Doğrulama yöntemi | SHA-256 imza hesabı | Veritabanı karşılaştırması |
| Veritabanı gerekir mi | Hayır | Evet (`persistent_logins`) |
| Cookie içeriği | username + expiration + signature | series + token |
| Şifre değişince | Token geçersiz olur | Etkilenmez |
| Hırsızlık tespiti | **Yapılamaz** | **Yapılabilir** (series eşleşir, token eşleşmez) |
| Token iptali | Şifre değiştirmek gerekir | DB kaydını silmek yeter |
| Kurulum kolaylığı | Çok kolay | Şema + repository gerekir |
| Temizlik gerekir mi | Hayır | **Evet** (manuel scheduler yazmalısın) |

### Öğrenilen anahtar sınıflar

| Sınıf / Arayüz | Görevi |
|---|---|
| `RememberMeAuthenticationFilter` | İsteği yakalar, remember-me sürecini başlatır |
| `RememberMeServices` | Cookie'yi inceler, doğrular, oluşturur, iptal eder |
| `TokenBasedRememberMeServices` | İmza tabanlı implementasyon |
| `PersistentTokenBasedRememberMeServices` | Veritabanı tabanlı implementasyon |
| `PersistentTokenRepository` | Token'ların nerede/nasıl saklandığını tanımlar |
| `JdbcTokenRepositoryImpl` | Hazır JDBC implementasyonu |
| `RememberMeAuthenticationToken` | Oluşturulan authentication nesnesi |
| `LogoutHandler` | `logout` metodunu sağlayan arayüz |

### Güvenlik kontrol listesi (üretim için)

1. `key` değeri en az 36 rastgele karakter, uygulama adı + ortam bilgisi içersin.
2. **SSL/TLS zorunlu** — cookie hırsızlığına karşı en güçlü tek önlem.
3. Hassas sayfalarda `isFullyAuthenticated()` kullan.
4. Servlet 5.0+ (Tomcat 10+) ile `HttpOnly` cookie otomatik gelir — bunu kullan.
5. XSS açıklarını kapat (OWASP Top Ten oku).
6. Persistent-based kullanıyorsan **temizleyici scheduler yaz** — cluster'da her JVM'de çalışacağını unutma.
7. Ekstra güvenlik istiyorsan token'ı IP'ye bağla (paylaşımlı ağ riskini göz önünde bulundur).

### Sıradaki bölüm

**Certificate-based authentication** — güvenilir istemci taraflı sertifikalarla kimlik doğrulama.

---

**Kendini test et:** Bu 5 soruya cevap verebiliyorsan bölümü öğrenmişsindir.

1. `JSESSIONID` ile `remember-me` cookie'si arasındaki fark nedir?
2. Token-based remember-me'de imza hangi 4 değerden hesaplanır?
3. Kullanıcı şifresini değiştirince token-based token'lara ne olur, neden?
4. Persistent-based'de series eşleşip token eşleşmezse ne anlama gelir ve sistem ne yapar?
5. `isFullyAuthenticated()` ile `isAuthenticated()` arasındaki fark, remember-me açısından neden önemlidir?

---

<a id="t2"></a>

## 2 · /cio:practice-exercises @spring-security/raw/ch07-remember_me_services.pdf [↑](#top)

# Pratik Alıştırmalar — Remember-Me Services

Bölümün tamamı zaten okundu; aşağıdaki 5 alıştırma o içeriğe dayanıyor. Hepsi eldeki `chapter07.xx-calendar` örnek projesi üzerinde yapılabilir. **Çözümleri yazmadım** — istediğinde söyle.

---

## Alıştırma 1 — "Kutu var ama çalışmıyor" (Kolay)

**Senaryo**

Bir ekip arkadaşın JBCP Calendar'a remember-me eklemiş. `SecurityConfig.java` içinde `http.rememberMe(...)` çağrısı duruyor, `login.html` içinde de "Remember Me?" checkbox'ı görünüyor. Ama kimse hatırlanmıyor: tarayıcı kapanıp açılınca herkes login sayfasına düşüyor. Arkadaşın HTML'i şöyle yazmış:

```html
<div class="mb-3">
    <label for="remember-me">Remember Me?</label>
    <input type="checkbox" id="remember-me" name="rememberMe" />
</div>
```

**Yapman gerekenler**

1. Buradaki hatayı bul ve düzelt.
2. Uygulamayı çalıştır, login ol ve DevTools → Application → Cookies ekranından **iki cookie'nin de** yazıldığını doğrula.
3. Şu iki senaryoyu ayrı ayrı dene ve sonuçları not al:
   - Sadece `JSESSIONID` cookie'sini sil, sayfayı yenile.
   - Sadece `remember-me` cookie'sini sil, sayfayı yenile.
4. İki denemenin sonucu neden farklı, tek cümleyle yaz.

---

## Alıştırma 2 — Hassas sayfayı remember-me'ye kapat (Kolay–Orta)

**Senaryo**

Ürün ekibi diyor ki: "Kullanıcıları hatırlayalım, takvimlerini görsünler. Ama **hesap ayarları** sayfasına girmek isteyen herkes şifresini yeniden girsin — cookie ile girilemesin."

Projede `/admin/h2/**` için benzer bir kural zaten var; sen bunu yeni bir yola uygulayacaksın.

**Yapman gerekenler**

1. `SecurityConfig.java` içinde `/events/my*` (veya senin projendeki uygun bir kullanıcı sayfası) için, kullanıcının **cookie ile değil, gerçek kullanıcı adı + şifre ile** giriş yapmış olmasını zorunlu kılan bir yetkilendirme kuralı yaz.
2. Şu akışı test et ve her adımda ne olduğunu not al:
   - Remember-me işaretli login ol → korumalı sayfaya gir → çalışmalı.
   - `JSESSIONID`'yi sil → **All Events** sayfasına gir → hâlâ çalışmalı.
   - Aynı durumda korumalı sayfaya gir → engellenmeli.
3. Bir de aynı kuralı, koşulu **yalnızca role dayalı** olacak şekilde yaz ve neden bunun ürün ekibinin isteğini karşılamadığını bir cümleyle açıkla.

---

## Alıştırma 3 — Anahtar ve süre kararları (Orta)

**Senaryo**

Uygulama üretime çıkıyor. Şu anda konfigürasyonda `key("jbcpCalendar")` yazıyor ve `tokenValiditySeconds` hiç ayarlanmamış. Ayrıca ekip üç ortam çalıştırıyor: `dev`, `test`, `prod` — ve test sırasında bir geliştirici, `test` ortamında aldığı cookie ile `dev` ortamına girebildiğini fark etti.

**Yapman gerekenler**

1. Üretime uygun bir `key` değeri üret ve konfigürasyona yaz. Değerin bölümde anlatılan üç şartı da karşılamalı (uzunluk, rastgelelik, ortam ayrımı).
2. Ortamlar arası cookie sızmasını engellemek için `key`'i ortam başına nasıl farklılaştıracağını uygula.
3. `tokenValiditySeconds` için iki ayrı deneme yap ve davranış farkını gözlemleyerek not al:
   - Kısa bir değer (örneğin 60 saniye) ver, süre dolduktan sonra sayfayı yenile.
   - `-1` ver, tarayıcıyı tamamen kapatıp aç.
4. İki denemede cookie'nin tarayıcıdaki **Expires / Max-Age** sütununun nasıl değiştiğini DevTools'tan kaydet.

---

## Alıştırma 4 — Çalınmış cookie'yi yakala (Orta–Zor)

**Senaryo**

Güvenlik ekibi bir soru soruyor: "Bir kullanıcının remember-me cookie'si çalınırsa bunu **fark edebiliyor muyuz**?" Şu anki uygulama token-based çalışıyor. Ekibe hem cevabı hem de kanıtını götürmen isteniyor.

**Yapman gerekenler**

1. Uygulamayı **persistent-based** remember-me'ye geçir (`persistent_logins` şeması + `PersistentTokenRepository` bean'i + `tokenRepository` bağlantısı).
2. Login ol ve H2 konsolundan `PERSISTENT_LOGINS` tablosunu sorgula. `series` ve `token` değerlerini bir yere kopyala.
3. Sayfayı birkaç kez yenile / oturumu birkaç kez otomatik olarak aç ve tabloyu tekrar sorgula. **Hangi sütun değişiyor, hangisi sabit kalıyor?** Not al.
4. Hırsızlığı simüle et: Adım 2'de kopyaladığın **eski** cookie değerini tarayıcıya geri yaz (DevTools'tan cookie değerini elle değiştir) ve sayfayı yenile.
5. Ne oldu? Tablodaki kayda ne oldu? Kullanıcının oturumuna ne oldu?
6. Aynı simülasyonu token-based konfigürasyonla tekrarlamayı dene ve neden aynı tespiti yapamadığını yaz.

---

## Alıştırma 5 — Sessizce büyüyen tablo (Zor)

**Senaryo**

Uygulama 6 aydır üretimde. DBA seni arıyor: `persistent_logins` tablosunda 400.000 satır var, çoğu aylardır kullanılmamış. Kimse silmiyor, çünkü Spring Security bunu kendi başına yapmıyor.

Ayrıca uygulama 3 sunuculuk bir cluster'da çalışıyor.

**Yapman gerekenler**

1. Süresi dolmuş kayıtları silen bir temizleyici yaz (`Runnable` olarak) ve bunu belirli aralıklarla çalıştıran bir zamanlayıcı tanımla.
2. Test edebilmek için aralığı çok kısa tut (örneğin 30 saniye) ve token geçerlilik süresini de kısa ver. Birkaç token oluştur, H2 konsolundan silinişlerini izle.
3. Temizleyicinin kullandığı repository metodunu dikkatle incele: **doğru kayıtları mı siliyor, yoksa tam tersini mi?** Gerekiyorsa düzelt ve düzeltmeni H2 sorgusuyla kanıtla.
4. Cluster sorununu ele al: bu zamanlayıcı 3 sunucuda birden çalışırsa ne olur? En az iki farklı çözüm yaklaşımı yaz (kod yazman şart değil, kararı gerekçesiyle anlat).
5. Temizleyicinin hata durumunda uygulamayı çökertmediğinden emin ol — repository'ye erişilemediğinde ne olduğunu test et.

---

## 🎯 Stretch Challenge — "Bu cookie sana ait değil"

**Senaryo**

Bir müşteri şu şikâyetle geldi: Kullanıcılarından birinin remember-me cookie'si bir halka açık bilgisayardan kopyalanmış ve başka bir şehirden kullanılmış. Müşteri iki şey istiyor:

1. Çalınan bir cookie **başka bir makineden kullanılamasın**.
2. Rakipler HTML kaynağına baktığında uygulamanın Spring Security kullandığını **anlayamasın**.

**Yapman gerekenler**

1. `PersistentTokenRepository` arayüzünü sarmalayan (decorator) özel bir implementasyon yaz. Bu sınıf, series identifier'ı **veritabanına yazarken** kullanıcının IP adresiyle birleştirsin, ama **cookie'ye yalnızca saf series** gitsin.
   - IP adresine `RequestContextHolder` üzerinden ulaş.
   - Gerekli filter bean'ini tanımlamayı unutma, yoksa request attributes `null` gelir.
2. Bu repository'yi `PersistentTokenBasedRememberMeServices` içine sar ve `rememberMeServices` ile konfigürasyona bağla.
3. Cookie adını ve form parametre adını varsayılan dışında bir şeyle değiştir. `login.html`'i de buna göre güncelle.
4. Testler:
   - Normal login → hatırlanma çalışmalı.
   - Cookie'yi kopyalayıp farklı bir IP'den (ikinci makine, VPN, veya `127.0.0.1` yerine makinenin LAN IP'si üzerinden) kullan → **reddedilmeli ve cookie silinmeli**.
   - `PERSISTENT_LOGINS` tablosundaki `series` sütununa bak: cookie'deki değerle **aynı mı, farklı mı?** Neden?
5. Son olarak bir risk notu yaz: Kullanıcılar kurumsal bir WAN, yük dengeleyici veya mobil ağ arkasındaysa bu çözüm ne zaman kullanıcıyı yanlışlıkla dışarı atar? Bu riski azaltmak için ne yapardın?

---
