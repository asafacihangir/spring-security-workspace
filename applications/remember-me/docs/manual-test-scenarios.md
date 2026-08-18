# Manuel Test Rehberi

Backend: `http://localhost:8080` · Frontend: `http://localhost:5173`

## Kullanıcılar

| Kullanıcı | Şifre | Kim? |
|---|---|---|
| user1@example.com | user1 | Normal kullanıcı (USER) |
| admin1@example.com | admin1 | Admin (ADMIN) |
| user2@example.com | user2 | Normal kullanıcı (USER) |

## 1. Login çalışıyor mu?

1. Çıkış yapmışken `/work-logs/my` aç → login sayfasına atmalı.
2. user1 ile gir → içeri almalı.
3. Yanlış şifreyle dene → hata göstermeli.
4. Logout yap → tekrar login istemeli.

## 2. Yetkiler doğru mu?

1. user1 ile "All Work Logs" (`/work-logs`) aç → **403** (sadece admin görür).
2. admin1 ile aç → 3 work log listelenmeli.
3. user1 ile work log **102**'nin detayına git → **403** (onu admin oluşturdu).
4. user1 ile work log **100**'e git → görünmeli (oluşturan o).

## 3. Remember-Me çalışıyor mu? (asıl test)

1. Login'de **Remember Me işaretsiz** gir → DevTools → Cookies: sadece `JSESSIONID` olmalı.
2. Logout yap, bu kez **Remember Me işaretli** gir → `remember-me` cookie'si de oluşmalı.
3. DevTools'tan **sadece `JSESSIONID`'yi sil**, sayfayı yenile → **hâlâ login olmalısın.** Remember-me'nin özü bu.
4. `remember-me` cookie'sinin değerini elle boz, `JSESSIONID`'yi de sil, yenile → login'e düşmelisin.
5. Remember-me ile girmişken logout yap → iki cookie de silinmeli.

## 4. Work log oluşturma

1. "Create Work Log" ile bir açıklama girip kaydet → My Work Logs'ta görünmeli; `createdBy` giriş yapan kullanıcı olmalı.
2. Explanation'ı boş bırakıp gönder → alan hatası göstermeli.

## 5. Signup

1. Yeni bir e-postayla kayıt ol → giriş yapıp korumalı sayfaları açabilmeli.
2. Mevcut bir e-postayla dene → hata göstermeli.

---

Bu adımların hepsi hem backend'de (8080) hem frontend'de (5173, React) aynı şekilde test edilir — ikisi aynı API'yi kullanır.
