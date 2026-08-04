# Use Case: Token Inspector

## Overview

**Use Case ID:** UC-013
**Use Case Name:** Token Inspector
**Primary Actor:** Öğrenen
**Goal:** Sunucu tarafındaki kalıcı hatırlanma kayıtlarını (series, token, son kullanım) canlı izleyerek token durumunu takip etmek ve hırsızlık senaryolarını hazırlamak
**Status:** Verified

## Preconditions

- Persistent-based remember-me stratejisi aktiftir
- Öğrenen uygulamaya erişebilmektedir

## Main Success Scenario

1. Öğrenen Token Inspector sayfasını açar.
2. Sistem mevcut kalıcı kayıtları kullanıcı adı, series, token ve son kullanım zamanı sütunlarıyla listeler.
3. Öğrenen başka bir sekmede remember-me davranışı tetikler (giriş, otomatik giriş veya logout).
4. Öğrenen Token Inspector sayfasını yeniler.
5. Sistem, yenileme sonrasında en geç 2 saniye içinde kayıtların güncel durumunu gösterir.
6. Öğrenen kayıt değişimini izleyerek token durumunu doğrular.

## Alternative Flows

### A1: Kayıt Bulunmuyor

**Trigger:** Henüz hiç kalıcı kayıt oluşmamıştır (step 2)
**Flow:**

1. Sistem boş liste durumunu açıklayan bir mesaj gösterir.
2. Öğrenen "Remember Me" seçeneğiyle giriş yaparak bir kayıt oluşturur.
3. Use case continues at step 1.

### A2: Token-Based Strateji Aktif

**Trigger:** Persistent mod kapalı olduğu için izlenecek sunucu kaydı yoktur (step 2)
**Flow:**

1. Öğrenen yapılandırmayı persistent moda çevirir (bkz. UC-010).
2. Use case continues at step 1.

## Postconditions

### Success Postconditions

- Öğrenen kalıcı kayıtların güncel durumunu görüntülemiştir
- Sayfa, kayıtlardaki değişiklikleri yenileme sonrası en geç 2 saniye içinde yansıtmaktadır

### Failure Postconditions

- Kayıtlar görüntülenememiş veya bayat gösterilmişse izleme yapılamamıştır; strateji ve sayfa davranışı incelenmelidir

## Business Rules

### BR-019: Tazelik Garantisi

Token Inspector, sayfa yenilendikten sonra en geç 2 saniye içinde kalıcı kayıtların güncel durumunu yansıtır.
