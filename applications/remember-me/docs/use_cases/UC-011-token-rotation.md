# Use Case: Token Rotation

## Overview

**Use Case ID:** UC-011
**Use Case Name:** Token Rotation
**Primary Actor:** Öğrenen
**Goal:** Her otomatik girişte kalıcı kaydın token değerinin yenilendiğini, series değerinin ise sabit kaldığını gözlemlemek
**Status:** Verified

## Preconditions

- Persistent-based remember-me stratejisi aktiftir
- Öğrenen "Remember Me" seçeneğiyle giriş yapmıştır ve sunucu tarafında bir kalıcı kayıt (series/token) mevcuttur

## Main Success Scenario

1. Öğrenen Token Inspector sayfasında mevcut kaydın series ve token değerlerini not eder.
2. Öğrenen oturumunu sonlandırır (remember-me cookie'sini koruyarak).
3. Öğrenen korumalı bir sayfaya istek yapar ve otomatik giriş gerçekleşir.
4. Sistem kalıcı kaydın token değerini yeni bir değerle değiştirir; series değeri aynı kalır.
5. Öğrenen Token Inspector'ı yeniler ve değerleri önceki notuyla karşılaştırır.
6. Öğrenen token'ın döndüğünü, series'in sabit kaldığını doğrular.

## Alternative Flows

### A1: Token-Based Strateji Aktif

**Trigger:** Aktif strateji persistent değildir ve Inspector'da kayıt yoktur (step 1)
**Flow:**

1. Öğrenen yapılandırmayı persistent moda çevirir (bkz. UC-010) ve yeniden giriş yapar.
2. Use case continues at step 1.

### A2: Otomatik Giriş Gerçekleşmedi

**Trigger:** Oturum hâlâ aktif olduğu için istek otomatik giriş tetiklemez (step 3)
**Flow:**

1. Öğrenen oturumun gerçekten sonlandığından emin olur (tarayıcı oturum cookie'sini siler).
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- Kalıcı kayıtta yeni bir token değeri, aynı series değeri bulunur
- Öğrenen series/token rotasyon davranışını doğrulamıştır

### Failure Postconditions

- Kayıt değerleri değişmemişse rotasyon gözlemlenememiştir; strateji ve akış gözden geçirilmelidir

## Business Rules

### BR-015: Kullanımda Rotasyon

Her başarılı otomatik girişte kaydın token değeri yenilenir; eski token değeri geçersiz hale gelir.

### BR-016: Sabit Series

Series değeri, kayıt oluşturulduğu andan silinene kadar değişmez ve kaydın kimliği olarak kullanılır.
