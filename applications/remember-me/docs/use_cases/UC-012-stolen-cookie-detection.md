# Use Case: Stolen Cookie Detection

## Overview

**Use Case ID:** UC-012
**Use Case Name:** Stolen Cookie Detection
**Primary Actor:** Öğrenen
**Goal:** Geçerli series ancak bayat token taşıyan bir isteğin cookie hırsızlığı olarak algılanıp tüm series'in iptal edildiğini gözlemlemek
**Status:** Draft

## Preconditions

- Persistent-based remember-me stratejisi aktiftir
- Öğrenen "Remember Me" seçeneğiyle giriş yapmıştır ve geçerli bir kalıcı kayıt mevcuttur

## Main Success Scenario

1. Öğrenen, hırsızlığı simüle etmek için mevcut remember-me cookie'sinin bir kopyasını alır.
2. Öğrenen meşru tarayıcıda oturumu düşürüp otomatik giriş yapar; sistem token değerini döndürür ve kopyadaki token bayatlar.
3. Öğrenen kopyalanan (bayat) cookie ile korumalı bir sayfaya istek yapar.
4. Sistem geçerli bir series ile eşleşmeyen bir token geldiğini tespit eder ve bunu cookie hırsızlığı olarak değerlendirir.
5. Sistem o series'e ait tüm kalıcı kayıtları siler.
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

- İlgili series'e ait tüm kalıcı kayıtlar silinmiştir
- Hem bayat cookie hem meşru cookie ile otomatik giriş artık mümkün değildir; yeniden login gerekir
- Öğrenen hırsızlık tespit davranışını gözlemlemiştir

### Failure Postconditions

- Kayıtlar silinmemiş ve bayat cookie kabul edilmişse tespit mekanizması çalışmamıştır; yapılandırma incelenmelidir

## Business Rules

### BR-017: Hırsızlık Varsayımı

Geçerli bir series ile birlikte güncel olmayan bir token gelmesi cookie hırsızlığı olarak yorumlanır; istek asla kabul edilmez.

### BR-018: Series Genelinde İptal

Hırsızlık tespitinde yalnızca ilgili istek değil, o series'e bağlı tüm hatırlanma kayıtları iptal edilir; kullanıcı her yerde yeniden giriş yapmak zorunda kalır.
