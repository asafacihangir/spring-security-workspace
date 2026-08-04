# Use Case: Auto-Login After Session Loss

## Overview

**Use Case ID:** UC-004
**Use Case Name:** Auto-Login After Session Loss
**Primary Actor:** Not Kullanıcısı
**Goal:** Oturum sona erdiğinde, kimlik bilgilerini yeniden girmeden remember-me cookie'siyle otomatik olarak doğrulanıp kaldığı yerden devam etmek
**Status:** Draft

## Preconditions

- Kullanıcı daha önce "Remember Me" seçeneğiyle giriş yapmıştır ve tarayıcısında geçerli bir remember-me cookie'si vardır
- Kullanıcının oturumu sona ermiştir (zaman aşımı veya sunucu yeniden başlatma)

## Main Success Scenario

1. Kullanıcı korumalı bir sayfaya istek yapar.
2. Sistem geçerli bir oturum bulunmadığını belirler.
3. Sistem istekle gelen remember-me cookie'sini doğrular.
4. Sistem kullanıcıyı otomatik olarak "Remembered" seviyesinde doğrular ve yeni bir oturum başlatır.
5. Sistem istenen sayfayı gösterir.
6. Sistem kimlik doğrulama seviyesini "Remembered" olarak gösterir; kullanıcı bilgi girmeden devam etmiştir.

## Alternative Flows

### A1: Cookie Yok veya Geçersiz

**Trigger:** İstekte remember-me cookie'si yoktur veya doğrulanamaz (step 3)
**Flow:**

1. Sistem otomatik giriş yapmaz ve kullanıcıyı login sayfasına yönlendirir.
2. Use case ends.

### A2: İstenen Sayfa Tam Doğrulama Gerektirir

**Trigger:** İstenen sayfa yalnızca tam kimlik doğrulamayla erişilebilirdir (step 5)
**Flow:**

1. Sistem sayfayı göstermez ve kullanıcıyı yeniden kimlik doğrulama sayfasına yönlendirir (bkz. UC-009).
2. Use case ends.

## Postconditions

### Success Postconditions

- Kullanıcının yeni bir oturumu vardır ve seviyesi "Remembered"tır
- Kullanıcı istediği sayfayı görüntülemektedir

### Failure Postconditions

- Otomatik giriş yapılmamıştır; kullanıcı Anonymous seviyesindedir ve login sayfasındadır

## Business Rules

### BR-006: Sınırlı Yetki

Remember-me ile elde edilen "Remembered" seviye, tam kimlik doğrulama gerektiren sayfalara erişim hakkı vermez.
