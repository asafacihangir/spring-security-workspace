# Use Case: Expired Token Rejection

## Overview

**Use Case ID:** UC-005
**Use Case Name:** Expired Token Rejection
**Primary Actor:** Öğrenen
**Goal:** Geçerlilik süresi dolmuş bir remember-me cookie'sinin otomatik girişte reddedildiğini gözlemlemek
**Status:** Draft

## Preconditions

- Uygulama, remember-me geçerlilik süresi kısa bir değere ayarlanacak şekilde yapılandırılabilir durumdadır
- Kayıtlı bir demo kullanıcı hesabı mevcuttur

## Main Success Scenario

1. Öğrenen, remember-me geçerlilik süresini kısa bir değere ayarlar ve uygulamayı başlatır.
2. Öğrenen "Remember Me" seçeneğiyle giriş yapar.
3. Öğrenen oturumu sonlandırıp geçerlilik süresi dolana kadar bekler.
4. Öğrenen korumalı bir sayfaya istek yapar.
5. Sistem remember-me cookie'sinin geçerlilik süresinin dolduğunu tespit eder.
6. Sistem otomatik girişi reddeder ve öğreneni login sayfasına yönlendirir.
7. Öğrenen, süresi dolmuş cookie'nin kabul edilmediğini gözlemler.

## Alternative Flows

### A1: Süre Henüz Dolmamış

**Trigger:** İstek anında geçerlilik süresi henüz dolmamıştır (step 5)
**Flow:**

1. Sistem otomatik girişi gerçekleştirir ve sayfayı gösterir.
2. Öğrenen sürenin dolmasını bekler.
3. Use case continues at step 4.

### A2: Oturum Hâlâ Aktif

**Trigger:** Bekleme sırasında oturum sona ermemiştir ve istek oturumla karşılanır (step 4)
**Flow:**

1. Öğrenen oturumu elle sonlandırır (tarayıcı oturum cookie'sini siler veya sunucuyu yeniden başlatır).
2. Use case continues at step 4.

## Postconditions

### Success Postconditions

- Süresi dolmuş cookie ile yapılan istek reddedilmiş, öğrenen login sayfasına yönlendirilmiştir
- Öğrenen geçerlilik süresi davranışını doğrulamıştır

### Failure Postconditions

- Otomatik giriş beklenmedik şekilde başarılı olmuşsa gözlem tamamlanmamıştır; yapılandırma gözden geçirilmelidir

## Business Rules

### BR-007: Kesin Geçerlilik Süresi

Yapılandırılan geçerlilik süresi dolduktan sonra remember-me cookie'si hiçbir istekte kabul edilmez.
