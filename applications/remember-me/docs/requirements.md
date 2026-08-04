# Remember-Me Lab — Requirements

Kaynak: [vision.md](vision.md). Roller: **not kullanıcısı** (demo kullanıcısı, `USER`),
**öğrenen** (uygulamayı çalıştıran geliştirici). "Simüle saldırgan" bir rol değil senaryodur;
ilgili gereksinimler öğrenen perspektifinden yazılmıştır.

## Functional Requirements (FR)

| ID     | Title                         | User Story                                                                                                                                                                                      | Priority | Status |
| ------ | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- | ------ |
| FR-001 | Form Login                    | Bir not kullanıcısı olarak, React login formu üzerinden kullanıcı adı ve şifremle giriş yapmak istiyorum; böylece kişisel notlarıma erişebilirim.                                               | High     | Done   |
| FR-002 | Remember-Me Opt-In            | Bir not kullanıcısı olarak, login formunda isteğe bağlı bir "Remember Me" checkbox'ı istiyorum; böylece `remember-me` cookie'si yalnızca ben seçtiğimde üretilir.                               | High     | Done   |
| FR-003 | Logout                        | Bir not kullanıcısı olarak, logout'un hem `JSESSIONID` hem `remember-me` cookie'lerini geçersiz kılmasını istiyorum; böylece sonraki istek yeniden login gerektirir.                            | High     | Done   |
| FR-004 | Auto-Login After Session Loss | Bir not kullanıcısı olarak, session'ım öldüğünde remember-me cookie'mden otomatik olarak yeniden authenticate edilmek istiyorum; böylece bilgilerimi girmeden devam edebilirim.                 | High     | Done   |
| FR-005 | Expired Token Rejection       | Bir öğrenen olarak, `tokenValiditySeconds` süresi aşılmış bir remember-me token'ıyla otomatik login'in reddedilmesini istiyorum; böylece token expiry davranışını gözlemlerim.                  | High     | Done   |
| FR-006 | Note Management               | Bir not kullanıcısı olarak, login sonrası notlarımı oluşturmak, listelemek, güncellemek ve silmek istiyorum; böylece remember-me'yi deneyecek gerçek bir akış olur.                             | Medium   | Done   |
| FR-007 | Auth Level Indicator          | Bir öğrenen olarak, UI'ın anlık authentication seviyemi (Anonymous / Remembered / Fully Authenticated) göstermesini istiyorum; böylece fark bir bakışta görünür olur.                           | Medium   | Done   |
| FR-008 | Sensitive Page Protection     | Bir not kullanıcısı olarak, Account Settings sayfasının full authentication (`isFullyAuthenticated()`) gerektirmesini istiyorum; böylece remembered bir session hesabımı değiştiremez.          | High     | Done   |
| FR-009 | Re-Authentication             | Bir not kullanıcısı olarak, remembered session'ımı şifremi yeniden girerek yükseltmek istiyorum; böylece Account Settings'e tekrar erişebilirim.                                                | Medium   | Done   |
| FR-010 | Strategy Switching            | Bir öğrenen olarak, token-based ve persistent-based remember-me stratejileri arasında geçiş yapmak istiyorum; böylece iki modun davranışını aynı uygulama üzerinde karşılaştırırım.             | Medium   | Done   |
| FR-011 | Token Rotation                | Bir öğrenen olarak, her kullanımda `persistent_logins` tablosundaki `token` değerinin dönmesini, `series` değerinin sabit kalmasını istiyorum; böylece series/token şemasını gözlemlerim.       | Medium   | Done   |
| FR-012 | Stolen Cookie Detection       | Bir öğrenen olarak, geçerli series ama bayat token taşıyan bir isteğin tüm series'i silmesini ve reddedilmesini istiyorum; böylece cookie hırsızlığı tespitini gözlemlerim.                     | Medium   | Done   |
| FR-013 | Token Inspector               | Bir öğrenen olarak, `persistent_logins` satırlarını (series, token, last_used) listeleyen bir Token Inspector sayfası istiyorum; böylece token durumunu canlı izleyip hırsızlığı simüle ederim. | Medium   | Done   |
| FR-014 | Expired Token Cleanup         | Bir öğrenen olarak, zamanlanmış bir arka plan işinin süresi dolmuş `persistent_logins` satırlarını silmesini istiyorum; böylece tablo sonsuza kadar büyümez.                                    | Low      | Done   |
| FR-015 | Custom Cookie & Param Names   | Bir öğrenen olarak, özel remember-me cookie ve HTTP parametre isimleri (ör. `notes-rm`, `keep-me`) istiyorum; böylece Spring Security'nin isimlendirme özelleştirme API'sini öğrenirim.         | Low      | Done   |
| FR-016 | IP-Bound Remember-Me          | Bir öğrenen olarak, remember-me token'ının üretildiği istemci IP'sine bağlanmasını istiyorum; böylece farklı IP'den replay reddedilir ve ek sertleştirme katmanını görürüm.                     | Low      | Done   |
| FR-017 | Token IP Visibility           | Bir öğrenen olarak, UI'ın token'ın hangi IP'ye bağlı olduğunu göstermesini istiyorum; böylece hırsızlık senaryosunda IP-binding davranışını doğrulayabilirim.                                   | Low      | Done   |

## Non-Functional Requirements (NFR)

| ID      | Title                       | Requirement                                                                                                                        | Category        | Priority | Status |
| ------- | --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | --------------- | -------- | ------ |
| NFR-001 | Cookie Protection           | `JSESSIONID` ve remember-me cookie'leri `HttpOnly` flag'i ile üretilmelidir.                                                       | Security        | High     | Done   |
| NFR-002 | Password Storage            | Şifreler BCrypt ile (strength ≥ 10) hash'lenerek saklanmalıdır; düz metin şifre asla persist edilmemelidir.                        | Security        | High     | Done   |
| NFR-003 | Cleanup Resilience          | Veritabanı erişilemezken cleanup işi hatayı loglamalı ve uygulama restart gerekmeden HTTP isteklerini karşılamaya devam etmelidir. | Availability    | Medium   | Done   |
| NFR-004 | Token Inspector Freshness   | Token Inspector sayfası, sayfa yenilendikten sonra en geç 2 saniye içinde güncel `persistent_logins` durumunu yansıtmalıdır.       | Performance     | Medium   | Done   |
| NFR-005 | Config-Only Strategy Switch | Token-based ↔ persistent-based mod geçişi yalnızca konfigürasyon değişikliği gerektirmelidir (0 satır Java değişikliği).           | Maintainability | Medium   | Done   |

## Constraints (C)

| ID    | Title              | Constraint                                                                                                          | Category  | Priority | Status |
| ----- | ------------------ | ------------------------------------------------------------------------------------------------------------------- | --------- | -------- | ------ |
| C-001 | Language & Runtime | Backend Java 21 üzerinde çalışmalıdır.                                                                              | Technical | High     | Done   |
| C-002 | Framework          | Backend Spring Boot 3.x ve Spring Security 6 kullanmalıdır.                                                         | Technical | High     | Done   |
| C-003 | Persistence        | Veriler Spring Data JPA (Hibernate) üzerinden MySQL'de saklanmalıdır.                                               | Technical | High     | Done   |
| C-004 | Infrastructure     | MySQL, `infra.yml` üzerinden Docker Compose ile ayağa kaldırılmalıdır.                                              | Technical | High     | Done   |
| C-005 | Frontend           | Frontend React 18 + Vite olmalı, cookie tabanlı session üzerinden sade fetch kullanmalıdır — JWT/token katmanı yok. | Technical | High     | Done   |
| C-006 | Build & Tasks      | Backend Maven, frontend npm ile derlenmeli; tüm geliştirme komutları Taskfile (`task`) üzerinden çalışmalıdır.      | Technical | Medium   | Done   |
| C-007 | Single Role        | Yetkilendirme tek `USER` rolüyle sınırlıdır; rol hiyerarşisi uygulanmaz.                                            | Technical | Medium   | Done   |
