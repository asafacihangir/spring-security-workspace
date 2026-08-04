# Use Case: Stolen Cookie Detection

## Overview

**Use Case ID:** UC-012
**Use Case Name:** Stolen Cookie Detection
**Primary Actor:** Öğrenen
**Goal:** Geçerli series ancak bayat token taşıyan bir isteğin cookie hırsızlığı olarak algılanıp kullanıcının tüm kalıcı hatırlanma kayıtlarının (tüm series'lerinin) iptal edildiğini gözlemlemek
**Status:** Verified

## Preconditions

- Persistent-based remember-me stratejisi aktiftir
- Öğrenen "Remember Me" seçeneğiyle giriş yapmıştır ve geçerli bir kalıcı kayıt mevcuttur

## Main Success Scenario

1. Öğrenen, hırsızlığı simüle etmek için mevcut remember-me cookie'sinin bir kopyasını alır.
2. Öğrenen meşru tarayıcıda oturumu düşürüp otomatik giriş yapar; sistem token değerini döndürür ve kopyadaki token bayatlar.
3. Öğrenen kopyalanan (bayat) cookie ile korumalı bir sayfaya istek yapar.
4. Sistem geçerli bir series ile eşleşmeyen bir token geldiğini tespit eder ve bunu cookie hırsızlığı olarak değerlendirir.
5. Sistem, kullanıcıya ait tüm series'lerdeki kalıcı kayıtları siler (yalnızca çalınan series'i değil).
6. Sistem isteği reddeder ve yeniden giriş ister.
7. Öğrenen, meşru tarayıcıdan gelen sonraki isteğin de artık otomatik giriş yapamadığını görerek tespit mekanizmasını doğrular.

## Alternative Flows

### A1: Kopya Hâlâ Güncel

**Trigger:** Kopyalanan cookie'deki token henüz döndürülmediği için günceldir ve giriş başarılı olur (step 4)
**Flow:**

1. Sistem otomatik girişi kabul eder ve token'ı döndürür.
2. Öğrenen, kopyanın bayatlaması için meşru tarayıcıda bir otomatik giriş daha tetikler.
3. Use case continues at step 3.

## Postconditions

### Success Postconditions

- Kullanıcının tüm series'lerine ait kalıcı kayıtlar silinmiştir (yalnızca çalınan series değil)
- Hem bayat cookie hem meşru cookie ile otomatik giriş artık mümkün değildir; yeniden login gerekir
- Öğrenen hırsızlık tespit davranışını gözlemlemiştir

### Failure Postconditions

- Kayıtlar silinmemiş ve bayat cookie kabul edilmişse tespit mekanizması çalışmamıştır; yapılandırma incelenmelidir

## Business Rules

### BR-017: Hırsızlık Varsayımı

Geçerli bir series ile birlikte güncel olmayan bir token gelmesi cookie hırsızlığı olarak yorumlanır; istek asla kabul edilmez.

### BR-018: Kullanıcı Genelinde İptal

Hırsızlık tespitinde yalnızca ilgili istek veya ilgili series değil, kullanıcının sahip olduğu TÜM series'lere ait hatırlanma kayıtları iptal edilir; kullanıcı her yerde (tüm cihaz/tarayıcılarda) yeniden giriş yapmak zorunda kalır. Bu, Spring Security'nin `PersistentTokenBasedRememberMeServices` bileşeninin yerleşik (`removeUserTokens`) davranışıdır — daha dar, yalnızca çalınan series'i iptal eden bir davranış bilinçli olarak tercih edilmemiştir; kullanıcı genelinde iptal, olası bir hırsızlık karşısında daha önleyici bir duruş sağlar.
