# Use Case: Strategy Switching

## Overview

**Use Case ID:** UC-010
**Use Case Name:** Strategy Switching
**Primary Actor:** Öğrenen
**Goal:** Token-based ve persistent-based remember-me stratejileri arasında yalnızca yapılandırma değiştirerek geçiş yapmak ve iki modun davranış farkını aynı uygulama üzerinde karşılaştırmak
**Status:** Draft

## Preconditions

- Uygulama iki stratejiden birini yapılandırmayla seçebilecek şekilde hazırlanmıştır
- Uygulama çalışır durumdadır ve aktif bir strateji vardır

## Main Success Scenario

1. Öğrenen o an aktif olan stratejiyi belirler.
2. Öğrenen yapılandırmada strateji modunu diğerine çevirir.
3. Öğrenen uygulamayı yeniden başlatır.
4. Öğrenen "Remember Me" seçeneğiyle giriş yapar.
5. Sistem remember-me davranışını yeni stratejiye göre uygular.
6. Öğrenen iki modun farkını gözlemler: persistent modda Token Inspector'da sunucu tarafı kayıt oluşur, token-based modda oluşmaz.

## Alternative Flows

### A1: Geçersiz Yapılandırma

**Trigger:** Yapılandırma değeri tanınmadığı için uygulama başlatılamaz (step 3)
**Flow:**

1. Sistem başlangıçta hatayı bildirir.
2. Öğrenen yapılandırmayı düzeltir.
3. Use case continues at step 3.

### A2: Eski Cookie ile Karışıklık

**Trigger:** Önceki stratejiden kalan remember-me cookie'si yeni modda doğrulanamaz (step 4)
**Flow:**

1. Sistem eski cookie'yi reddeder ve login ister.
2. Öğrenen normal şekilde yeniden giriş yapar.
3. Use case continues at step 5.

## Postconditions

### Success Postconditions

- Uygulama seçilen yeni stratejiyle çalışmaktadır
- Öğrenen iki stratejinin davranış farkını aynı uygulamada gözlemlemiştir

### Failure Postconditions

- Uygulama önceki stratejide kalmıştır veya başlatılamamıştır; kod tabanında hiçbir değişiklik yapılmamıştır

## Business Rules

### BR-013: Yalnızca Yapılandırmayla Geçiş

Strateji geçişi yalnızca yapılandırma değişikliği gerektirir; hiçbir kaynak kodu değişikliği yapılmaz.

### BR-014: Tek Aktif Strateji

Uygulama aynı anda yalnızca tek bir remember-me stratejisiyle çalışır.
