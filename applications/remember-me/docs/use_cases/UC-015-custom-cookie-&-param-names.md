# Use Case: Custom Cookie & Param Names

## Overview

**Use Case ID:** UC-015
**Use Case Name:** Custom Cookie & Param Names
**Primary Actor:** Öğrenen
**Goal:** Remember-me cookie'sine ve login formundaki hatırlanma parametresine özel isimler (ör. `notes-rm`, `keep-me`) vererek isimlendirme özelleştirmesini gözlemlemek
**Status:** Verified

## Preconditions

- Uygulama, cookie ve parametre isimlerinin yapılandırmadan belirlenmesine izin verecek şekilde hazırlanmıştır

## Main Success Scenario

1. Öğrenen yapılandırmada özel cookie adını (`notes-rm`) ve özel parametre adını (`keep-me`) tanımlar.
2. Öğrenen uygulamayı yeniden başlatır.
3. Öğrenen "Remember Me" seçeneğiyle giriş yapar; form hatırlanma tercihini özel parametre adıyla iletir.
4. Sistem remember-me cookie'sini özel adla üretir.
5. Öğrenen tarayıcı geliştirici araçlarında cookie'nin özel adla oluştuğunu doğrular.

## Alternative Flows

### A1: Özel İsim Tanımlı Değil

**Trigger:** Yapılandırmada özel isim eksik olduğu için varsayılan isimler kullanılmıştır (step 4)
**Flow:**

1. Öğrenen cookie'nin varsayılan adla üretildiğini görür.
2. Öğrenen yapılandırmayı düzeltir.
3. Use case continues at step 2.

### A2: Parametre Adı Uyuşmuyor

**Trigger:** Form, sistemin beklediğinden farklı bir parametre adı gönderdiği için hatırlanma tercihi işlenmez (step 3)
**Flow:**

1. Sistem girişi kabul eder ancak remember-me cookie'si üretmez.
2. Öğrenen form ve yapılandırma isimlerini eşitler.
3. Use case continues at step 2.

## Postconditions

### Success Postconditions

- Remember-me cookie'si yapılandırılan özel adla üretilmektedir
- Hatırlanma tercihi özel parametre adıyla iletilmekte ve işlenmektedir

### Failure Postconditions

- Cookie varsayılan adla üretilmiş veya hiç üretilmemiştir; öğrenen yapılandırmayı gözden geçirmelidir

## Business Rules

### BR-022: İsimler Yapılandırmadan Yönetilir

Remember-me cookie adı ve form parametre adı yalnızca yapılandırmadan belirlenir; frontend ve backend aynı isimleri kullanmak zorundadır.
