# Use Case: Logout

## Overview

**Use Case ID:** UC-003
**Use Case Name:** Logout
**Primary Actor:** Not Kullanıcısı
**Goal:** Oturumu ve hatırlanma durumunu tamamen sonlandırmak; sonraki erişimlerin yeniden giriş gerektirmesini sağlamak
**Status:** Draft

## Preconditions

- Kullanıcı giriş yapmış durumdadır (Fully Authenticated veya Remembered)

## Main Success Scenario

1. Kullanıcı "Logout" seçeneğini seçer.
2. Sistem kullanıcının oturumunu sonlandırır.
3. Sistem hem oturum cookie'sini hem remember-me cookie'sini geçersiz kılar.
4. Sistem, kullanıcıya ait sunucu tarafı hatırlanma kayıtlarını siler.
5. Sistem kullanıcıyı login sayfasına yönlendirir.
6. Kullanıcının korumalı bir sayfaya sonraki isteği yeniden giriş gerektirir; kullanıcı tamamen çıkış yapmıştır.

## Alternative Flows

### A1: Oturum Zaten Sona Ermiş

**Trigger:** Logout isteği geldiğinde oturum sunucuda zaten geçersizdir (step 2)
**Flow:**

1. Sistem yine de tarayıcıdaki oturum ve remember-me cookie'lerini geçersiz kılar ve varsa hatırlanma kayıtlarını siler.
2. Sistem kullanıcıyı login sayfasına yönlendirir.
3. Use case ends.

## Postconditions

### Success Postconditions

- Kullanıcının oturumu ve remember-me cookie'si geçersizdir; sunucu tarafı hatırlanma kaydı kalmamıştır
- Kullanıcı Anonymous seviyesindedir ve login sayfasındadır

### Failure Postconditions

- Cookie'lerden en az biri geçersiz kılınamamışsa kullanıcı çıkışın tamamlanmadığını görür; korumalı sayfalara erişim yine de yeniden doğrulama gerektirir

## Business Rules

### BR-005: Tam Çıkış

Logout sonrasında hiçbir mevcut cookie ile — oturum veya remember-me — otomatik giriş yapılamaz; erişim ancak yeni bir login ile mümkündür.
